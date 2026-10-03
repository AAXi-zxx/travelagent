package com.travel.ai.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.ai.openai.OpenAiChatOptions;
import com.travel.ai.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存工具调用信息的响应结果（要调用那些工具）
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = OpenAiChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        // 1、校验提示词，拼接用户提示词
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        // 2、调用 AI 大模型，获取工具调用结果
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，用于等下 Act
            this.toolCallChatResponse = chatResponse;
            // 3、解析工具调用结果，获取要调用的工具
            // 助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息
            String result = assistantMessage.getText();
            // 记录思考文本，供 ReActAgent 在无需行动时把回答内容返回给调用方
            this.latestThinkResult = result == null ? "" : result;
            log.info(getName() + "的思考：" + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            // 如果不需要调用工具，返回 false
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                // 无工具调用说明大模型已给出最终回答，直接结束循环，避免无谓地继续跑到 maxSteps
                setState(AgentState.FINISHED);
                return false;
            } else {
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题：" + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具需要调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            // 任务结束，更改状态
            setState(AgentState.FINISHED);
        }
        // 调用终止工具时，直接返回大模型的最终回答；没有回答则返回简洁的结束提示
        if (terminateToolCalled) {
            if (toolCallChatResponse != null && toolCallChatResponse.getResult() != null) {
                String finalAnswer = toolCallChatResponse.getResult().getOutput().getText();
                if (finalAnswer != null && !finalAnswer.isBlank()) {
                    log.info(finalAnswer);
                    return finalAnswer;
                }
            }
            return "任务结束";
        }
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> formatToolResult(response.name(), response.responseData()))
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }

    /**
     * 格式化工具返回结果。
     * <p>
     * 图片搜索工具返回的是裸图片链接列表，这里转成 Markdown 图片语法（每张一行），
     * 使前端能直接渲染为整齐的缩略图，而不是一长串链接。
     */
    private String formatToolResult(String toolName, String responseData) {
        if ("searchImage".equals(toolName)) {
            // responseData 可能是真实换行，也可能是被转义的字面量 \n，统一按换行拆分
            String normalized = responseData.replace("\\n", "\n");
            String[] urls = normalized.split("\n");
            StringBuilder sb = new StringBuilder();
            sb.append("已搜索到 ").append(urls.length).append(" 张图片：\n");
            for (String url : urls) {
                String trimmed = url.trim();
                // 去掉可能的首尾引号（工具结果经 JSON 序列化可能带引号）
                if (trimmed.startsWith("\"")) trimmed = trimmed.substring(1);
                if (trimmed.endsWith("\"")) trimmed = trimmed.substring(0, trimmed.length() - 1);
                trimmed = trimmed.trim();
                if (!trimmed.isEmpty()) {
                    sb.append("![").append("图片").append("](").append(trimmed).append(")\n");
                }
            }
            return sb.toString();
        }
        if ("searchWeb".equals(toolName)) {
            // WebSearchTool 已返回干净的「标题+链接+摘要」格式，只换一个更自然的前缀
            String trimmed = responseData.trim();
            // 去掉 JSON 序列化可能带来的首尾引号
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            trimmed = trimmed.replace("\\n", "\n").replace("\\\"", "\"");
            return "已搜索到以下网络资料：\n" + trimmed;
        }
        return "工具 " + toolName + " 返回的结果：" + responseData;
    }
}

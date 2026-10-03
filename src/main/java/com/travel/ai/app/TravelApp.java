package com.travel.ai.app;

import com.travel.ai.advisor.MyLoggerAdvisor;
import com.travel.ai.advisor.ReReadingAdvisor;
import com.travel.ai.chatmemory.FileBasedChatMemory;
import com.travel.ai.rag.TravelAppRagCustomAdvisorFactory;
import com.travel.ai.rag.QueryRewriter;
import com.travel.ai.tools.ImageSearchTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class TravelApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "扮演深耕旅游规划领域的专家。开场向用户表明身份，告知用户可咨询出行规划问题。" +
            "围绕交通、住宿、行程三个方向提问：交通方面询问出发地、预算区间与可接受的通勤时长；" +
            "住宿方面询问同行人数、对位置与安静程度的偏好；" +
            "行程方面询问出行天数、同行人体力状况与必去目的地。" +
            "引导用户说明出行时间、同行人构成与总预算，以便给出可落地的行程方案。";

    // 统一智能体补充提示：明确告知模型具备的工具能力，引导其在合适场景调用
    private static final String UNIFIED_TOOL_HINT =
            "你还可以调用工具来完成任务：当用户想要查找/搜索图片时，调用图片搜索工具（searchImage）。" +
            "重要：用户看不到工具的返回结果，只有你能看到。因此你必须把工具返回的每一条图片链接" +
            "原样复制到你的回答正文中，格式为 ![图片](链接)，每行一条，一条都不能省略。" +
            "绝对不要只说「已为你找好图片」而不贴出链接——那样的回答是无效的。" +
            "其他与旅游规划无关、或知识库无法回答的问题，再考虑使用联网搜索工具。";

    /**
     * 初始化 ChatClient
     *
     * @param chatModel
     */
    public TravelApp(ChatModel chatModel) {
//        // 初始化基于文件的对话记忆
//        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor()
//                        // 自定义推理增强 Advisor，可按需开启
//                       ,new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    record TravelReport(String title, List<String> suggestions) {

    }

    /**
     * AI 旅游规划功能（实战结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public TravelReport doChatWithReport(String message, String chatId) {
        TravelReport travelReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成旅游规划方案，标题为{目的地}的行程规划，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(TravelReport.class);
        log.info("travelReport: {}", travelReport);
        return travelReport;
    }

    // AI 旅游知识库问答功能

    @Resource
    private VectorStore travelAppVectorStore;

    // PGVector 向量存储：对应配置类 PgVectorVectorStoreConfig 已整体注释（未启用），
    // 该注入悬空会导致启动失败，故一并注释；启用 PGVector 时再恢复
    // @Resource
    // private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 和 RAG 知识库进行对话
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        // 查询重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                // 使用改写后的查询
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用 RAG 知识库问答
                .advisors(new QuestionAnswerAdvisor(travelAppVectorStore))
                // 应用 RAG 检索增强服务（基于 PgVector 向量存储）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
//                .advisors(
//                        TravelAppRagCustomAdvisorFactory.createTravelAppRagCustomAdvisor(
//                                travelAppVectorStore, "交通"
//                        )
//                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    /**
     * AI 旅游规划功能（支持调用工具）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用 MCP 服务

    // MCP 客户端已启用（application.yml 的 mcp.client.sse 已打开），
    // ToolCallbackProvider Bean 由 spring-ai-starter-mcp-client 提供，注入即可用。
    // 使用前需先单独启动 image-search-mcp-server（端口 8127）。
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 旅游规划功能（调用 MCP 服务）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 统一智能体对话（流式）：大模型自主决策。
     * 同时具备多轮记忆 + RAG 知识库检索 + 内置工具调用，
     * 需要查资料时检索语料、需要动手时调用工具、普通聊天直接回答。
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatUnifiedByStream(String message, String chatId) {
        // 注：Spring AI 1.0 的 stream() + 工具自动执行会把「模型↔工具」中间轮次的文本
        // 重复输出到流中（前端表现为回答重复多遍）。因此这里先用 call() 完整跑完
        // 工具调用循环拿到最终回答，再一次性推给前端。TODO: 后续可升级为
        // 手动工具循环 + 仅对最终回答流式输出，以恢复打字机体验。
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .system(UNIFIED_TOOL_HINT)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // RAG 知识库检索（自动把相关语料注入上下文）
                .advisors(new QuestionAnswerAdvisor(travelAppVectorStore))
                // 内置工具（大模型自主决定是否调用）
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("unified content: {}", content);
        // 兜底：模型有时会只说「已找到图片」而不贴链接（qwen 常见问题），
        // 此时若本轮调用了图片搜索，程序化把图片链接追加到回答末尾
        List<String> imageUrls = ImageSearchTool.consumeLastResults();
        if (!imageUrls.isEmpty() && (content == null || !content.contains("images.pexels.com"))) {
            StringBuilder extra = new StringBuilder("\n\n已为你找到 ")
                    .append(imageUrls.size()).append(" 张图片：\n");
            for (String url : imageUrls) {
                extra.append("![图片](").append(url).append(")\n");
            }
            content = (content == null ? "" : content) + extra;
        }
        return Flux.just(content);
    }

    /**
     * 统一智能体对话（同步版，便于调试与测试）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatUnified(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .system(UNIFIED_TOOL_HINT)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new QuestionAnswerAdvisor(travelAppVectorStore))
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}

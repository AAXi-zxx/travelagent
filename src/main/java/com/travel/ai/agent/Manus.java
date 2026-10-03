package com.travel.ai.agent;

import com.travel.ai.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Manus 自主规划智能体（拥有自主规划能力，可以直接使用）
 */
@Component
public class Manus extends ToolCallAgent {

    public Manus(ToolCallback[] allTools, ChatModel chatModel) {
        super(allTools);
        this.setName("manus");
        String SYSTEM_PROMPT = """
                You are Manus, an all-capable AI assistant that can handle ANY task the user asks —
                general knowledge Q&A, social media and product research, image search, document processing, and more.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests,
                such as searching the web for any information, searching and downloading images,
                reading and writing files, operating the terminal, and generating PDF documents.
                You are especially good at travel planning (attractions, transport, accommodation, itineraries),
                but travel is only one of your strengths, not your limitation.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                When presenting image search results, output each image link on its own line using Markdown image syntax: ![image](URL), one per line, and always list every link returned by the tool.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}

package com.travel.ai.controller;

import com.travel.ai.agent.Manus;
import com.travel.ai.app.TravelApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private TravelApp travelApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel chatModel;

    /**
     * 同步调用 AI 旅游大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/travel_app/chat/sync")
    public String doChatWithTravelAppSync(String message, String chatId) {
        return travelApp.doChat(message, chatId);
    }

    /**
     * 同步调用 AI 旅游大师知识库问答（RAG，基于内存向量库 + 本地 Ollama 向量化）
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/travel_app/chat/rag")
    public String doChatWithTravelAppRag(String message, String chatId) {
        return travelApp.doChatWithRag(message, chatId);
    }

    /**
     * 同步调用 AI 旅游大师工具能力（MCP）
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/travel_app/chat/mcp")
    public String doChatWithTravelAppMcp(String message, String chatId) {
        return travelApp.doChatWithMcp(message, chatId);
    }

    /**
     * 同步调用 AI 旅游大师内置工具（联网搜索/网页抓取/资源下载/PDF 生成等，无需外部 key）
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/travel_app/chat/tools")
    public String doChatWithTravelAppTools(String message, String chatId) {
        return travelApp.doChatWithTools(message, chatId);
    }

    /**
     * 统一智能体对话（流式）：大模型自主决定工具调用与知识库检索
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/travel_app/chat/unified", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithTravelAppUnified(String message, String chatId) {
        return travelApp.doChatUnifiedByStream(message, chatId);
    }

    /**
     * SSE 流式调用 AI 旅游大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/travel_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithTravelAppSSE(String message, String chatId) {
        return travelApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 流式调用 AI 旅游大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/travel_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithTravelAppServerSentEvent(String message, String chatId) {
        return travelApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式调用 AI 旅游大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/travel_app/chat/sse_emitter")
    public SseEmitter doChatWithTravelAppServerSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 SseEmitter
        travelApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        // 返回
        return sseEmitter;
    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        Manus manus = new Manus(allTools, chatModel);
        return manus.runStream(message);
    }
}

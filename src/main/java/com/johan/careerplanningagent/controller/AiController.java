package com.johan.careerplanningagent.controller;

import com.johan.careerplanningagent.agent.AIManus;
import com.johan.careerplanningagent.app.CareerPlanningAgentApp;
import com.johan.careerplanningagent.model.ApiResponse;
import com.johan.careerplanningagent.model.ChatReply;
import com.johan.careerplanningagent.model.ChatRequest;
import com.johan.careerplanningagent.service.ConversationService;
import jakarta.validation.Valid;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private ConversationService conversationService;

    @Resource
    private AIManus aiManus;

    @Resource
    private CareerPlanningAgentApp careerPlanningAgentApp;

    /**
     * 同步调用Ai 职业规划应用
     * @param message
     * @param chatId
     * @return
     */
    @PostMapping("/career_app/chat/sync")
    public ApiResponse<ChatReply> chat(@Valid @RequestBody ChatRequest request) {
        ChatReply reply = conversationService.chat(request.message(), request.chatId());
        return ApiResponse.success(reply);
    }

    /**
     * SSE 流式调用Ai 职业规划应用
     */
    @PostMapping(value = "/career_app/chat/sse",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithSse(@Valid @RequestBody ChatRequest request){
        return conversationService.chatStream(request.message(), request.chatId());
    }

    /**
     * 流式调用Manus智能体
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message){
        return aiManus.runStream(message);
    }

    @PostMapping("/career_app/chat/rag")
    public ApiResponse<ChatReply> chatWithRag(@Valid @RequestBody ChatRequest request) {
        String chatId = conversationService.ensureChatId(request.chatId());
        String ragReply = careerPlanningAgentApp.doChatWithRag(request.message(), chatId);
        return ApiResponse.success(new ChatReply(chatId, ragReply));
    }

    @PostMapping("/career_app/chat/tool")
    public ApiResponse<ChatReply> chatWithTool(@Valid @RequestBody ChatRequest request) {
        String chatId = conversationService.ensureChatId(request.chatId());
        String toolReply = careerPlanningAgentApp.doChatWithTool(request.message(), chatId);
        return ApiResponse.success(new ChatReply(chatId, toolReply));
    }
}

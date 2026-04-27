package com.johan.careerplanningagent.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.johan.careerplanningagent.agent.AIManus;
import com.johan.careerplanningagent.app.CareerPlanningAgentApp;
import com.johan.careerplanningagent.model.ApiResponse;
import com.johan.careerplanningagent.model.ChatReply;
import com.johan.careerplanningagent.model.ChatRequest;
import com.johan.careerplanningagent.model.KnowledgeFileInfo;
import com.johan.careerplanningagent.model.ReactTraceReply;
import com.johan.careerplanningagent.service.ConversationService;
import com.johan.careerplanningagent.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private ConversationService conversationService;

    @Resource
    private AIManus aiManus;

    @Resource
    private CareerPlanningAgentApp careerPlanningAgentApp;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    //AI调用工具能力
    @Resource
    private ToolCallback[] toolCallbacks;
    @Autowired
    private DashScopeChatModel dashscopeChatModel;

    /**
     * 同步调用Ai 职业规划应用
     * @param request
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
        AIManus aiManus1 = new AIManus(toolCallbacks, dashscopeChatModel);
        return aiManus1.runStream(message);
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

    @GetMapping("/conversations")
    public ApiResponse<List<String>> listConversations() {
        return ApiResponse.success(conversationService.listConversations());
    }

    @GetMapping("/conversations/{chatId}/history")
    public ApiResponse<List<String>> getConversationHistory(@PathVariable String chatId) {
        return ApiResponse.success(conversationService.getHistory(chatId));
    }

    @DeleteMapping("/conversations/{chatId}")
    public ApiResponse<Boolean> clearConversation(@PathVariable String chatId) {
        return ApiResponse.success(conversationService.clearConversation(chatId));
    }

    @PostMapping(value = "/knowledge/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<KnowledgeFileInfo> uploadKnowledge(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(knowledgeBaseService.upload(file));
    }

    @GetMapping("/knowledge/files")
    public ApiResponse<List<KnowledgeFileInfo>> listKnowledgeFiles() {
        return ApiResponse.success(knowledgeBaseService.listFiles());
    }

    @PostMapping("/knowledge/reindex")
    public ApiResponse<Integer> reindexKnowledgeBase() {
        return ApiResponse.success(knowledgeBaseService.reindexAll());
    }

    @PostMapping("/react/process")
    public ApiResponse<ReactTraceReply> reactProcess(@Valid @RequestBody ChatRequest request) {
        AIManus agent = new AIManus(toolCallbacks, dashscopeChatModel);
        String trace = agent.run(request.message());
        String chatId = conversationService.ensureChatId(request.chatId());
        return ApiResponse.success(new ReactTraceReply(chatId, trace, trace));
    }
}

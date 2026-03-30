package com.johan.careerplanningagent.controller;

import com.johan.careerplanningagent.agent.AIManus;
import com.johan.careerplanningagent.app.CareerPlanningAgentApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
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
    private CareerPlanningAgentApp careerPlanningAgentApp;

    @Resource
    private AIManus aiManus;

    @Resource
    private ChatModel  dashscopeChatModel;

    @Resource
    private ToolCallback[] allTools;

    /**
     * 同步调用Ai 职业规划应用
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/career_app/chat/sync")
    public String chat(String message,String chatId) {
        return careerPlanningAgentApp.chat(message,chatId);
    }

    /**
     * SSE 流式调用Ai 职业规划应用
     */
    @GetMapping(value = "/career_app/chat/sse",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithSse(String message, String chatId){
        return careerPlanningAgentApp.doChatByStream(message,chatId);
    }

    /**
     * 流式调用Manus智能体
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message){
        AIManus aiManus1 = new AIManus(allTools,dashscopeChatModel);
        return aiManus1.runStream(message);
    }
}

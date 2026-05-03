package com.johan.careerplanningagent.controller;

import com.johan.careerplanningagent.agent.AIManus;
import com.johan.careerplanningagent.service.ConversationService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping(value = "/career_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithSse(@RequestParam String message,
                                                       @RequestParam(required = false) String chatId) {
        return conversationService.chatStream(message, chatId)
                .map(chunk -> ServerSentEvent.builder(chunk).build())
                .concatWithValues(ServerSentEvent.<String>builder("[DONE]").event("done").build());
    }

    @GetMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithManus(@RequestParam String message) {
        return aiManus.runStream(message);
    }
}

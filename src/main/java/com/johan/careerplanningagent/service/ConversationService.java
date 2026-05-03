package com.johan.careerplanningagent.service;

import com.johan.careerplanningagent.app.CareerPlanningAgentApp;
import com.johan.careerplanningagent.exception.BusinessException;
import com.johan.careerplanningagent.model.ChatReply;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationService {

    private final CareerPlanningAgentApp careerPlanningAgentApp;
    private final PersistentMemoryService persistentMemoryService;
    private final Map<String, Instant> conversationRegistry = new ConcurrentHashMap<>();

    public ConversationService(CareerPlanningAgentApp careerPlanningAgentApp,
                               PersistentMemoryService persistentMemoryService) {
        this.careerPlanningAgentApp = careerPlanningAgentApp;
        this.persistentMemoryService = persistentMemoryService;
    }

    public ChatReply chat(String message, String chatId) {
        String resolvedChatId = ensureChatId(chatId);
        String content = careerPlanningAgentApp.chat(message, resolvedChatId);
        conversationRegistry.put(resolvedChatId, Instant.now());
        return new ChatReply(resolvedChatId, content);
    }

    public Flux<String> chatStream(String message, String chatId) {
        String resolvedChatId = ensureChatId(chatId);
        conversationRegistry.put(resolvedChatId, Instant.now());
        return careerPlanningAgentApp.doChatByStream(message, resolvedChatId);
    }

    public String ensureChatId(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        if (chatId.length() > 64) {
            throw new BusinessException(400, "chatId 長度不能超過 64");
        }
        return chatId;
    }

    public List<String> getHistory(String chatId) {
        String resolvedChatId = ensureChatId(chatId);
        return persistentMemoryService.loadHistory(resolvedChatId);
    }

    public List<String> listConversations() {
        return persistentMemoryService.listChatIds();
    }

    public boolean clearConversation(String chatId) {
        String resolvedChatId = ensureChatId(chatId);
        conversationRegistry.remove(resolvedChatId);
        return persistentMemoryService.clearHistory(resolvedChatId);
    }
}

package com.johan.careerplanningagent.config;

import com.johan.careerplanningagent.chatmemory.FileBasedChatMemory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory() {
        String fileDir = System.getProperty("user.dir") + "/chat-memory";
        return new FileBasedChatMemory(fileDir, 20);
    }
}

package com.johan.careerplanningagent.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PersistentMemoryService {

    private final Path baseDir = Path.of(System.getProperty("user.dir"), "chat-memory");

    public PersistentMemoryService() throws IOException {
        Files.createDirectories(baseDir);
    }

    public synchronized String buildMessageWithHistory(String chatId, String message, int maxLines) {
        List<String> history = loadRecent(chatId, maxLines);
        if (history.isEmpty()) {
            return message;
        }
        return "以下是历史对话，请结合上下文回答：\n"
                + String.join("\n", history)
                + "\n用户最新问题：" + message;
    }

    public synchronized void append(String chatId, String message, String response) {
        Path file = baseDir.resolve(chatId + ".log");
        String payload = "USER: " + normalizeLine(message) + "\n"
                + "ASSISTANT: " + normalizeLine(response) + "\n";
        try {
            Files.writeString(file, payload, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    public synchronized List<String> loadHistory(String chatId) {
        return loadRecent(chatId, Integer.MAX_VALUE);
    }

    public synchronized List<String> listChatIds() {
        if (!Files.exists(baseDir)) {
            return List.of();
        }
        try (Stream<Path> pathStream = Files.list(baseDir)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".log"))
                    .map(name -> name.substring(0, name.length() - 4))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    public synchronized boolean clearHistory(String chatId) {
        Path file = baseDir.resolve(chatId + ".log");
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            return false;
        }
    }

    private List<String> loadRecent(String chatId, int maxLines) {
        Path file = baseDir.resolve(chatId + ".log");
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            List<String> all = Files.readAllLines(file, StandardCharsets.UTF_8);
            int start = Math.max(0, all.size() - maxLines);
            return new ArrayList<>(all.subList(start, all.size()));
        } catch (IOException e) {
            return List.of();
        }
    }

    private String normalizeLine(String input) {
        return input == null ? "" : input.replace("\n", " ").replace("\r", " ");
    }
}

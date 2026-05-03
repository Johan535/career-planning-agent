package com.johan.careerplanningagent.controller;

import com.johan.careerplanningagent.agent.AIManus;
import com.johan.careerplanningagent.exception.BusinessException;
import com.johan.careerplanningagent.service.ConversationService;
import com.johan.careerplanningagent.service.GeneratedFileStore;
import jakarta.annotation.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/ai")
public class AiController {
    @Resource
    private ConversationService conversationService;

    @Resource
    private AIManus aiManus;

    @Resource
    private GeneratedFileStore generatedFileStore;

    @GetMapping(value = "/career_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithSse(@RequestParam String message,
                                                       @RequestParam(required = false) String chatId) {
        return conversationService.chatStream(message, chatId)
                .map(chunk -> ServerSentEvent.builder(chunk).build())
                .concatWithValues(ServerSentEvent.<String>builder("[DONE]").event("done").build());
    }

    /**
     * Manus 流式对话；chatId 与生成文件归属一致，用于后续下载校验。
     */
    @GetMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithManus(@RequestParam String message,
                                      @RequestParam(required = false) String chatId) {
        String sessionKey = (chatId == null || chatId.isBlank())
                ? "anonymous"
                : conversationService.ensureChatId(chatId);
        return aiManus.runStream(message, sessionKey);
    }

    /**
     * 下载工具生成的 PDF（chatId 须与生成时一致）。
     */
    @GetMapping(value = "/files/pdf/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<FileSystemResource> downloadPdf(@RequestParam String fileId,
                                                           @RequestParam(required = false) String chatId) {
        String sessionKey = (chatId == null || chatId.isBlank())
                ? "anonymous"
                : conversationService.ensureChatId(chatId);
        GeneratedFileStore.FileMeta meta = generatedFileStore.require(fileId, sessionKey);
        FileSystemResource body = new FileSystemResource(meta.absolutePath().toFile());
        if (!body.exists()) {
            throw new BusinessException(404, "文件实体不存在或已被清理");
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(meta.originalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}

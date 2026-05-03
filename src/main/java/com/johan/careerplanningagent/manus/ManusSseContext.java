package com.johan.careerplanningagent.manus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * 当前 Manus SSE 请求上下文（与执行智能体的线程一致），供工具推送 artifact 等事件。
 */
public final class ManusSseContext {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    public record Context(String sessionKey, SseEmitter emitter) {
    }

    private ManusSseContext() {
    }

    public static void open(String sessionKey, SseEmitter emitter) {
        String sk = sessionKey == null || sessionKey.isBlank() ? "anonymous" : sessionKey;
        HOLDER.set(new Context(sk, emitter));
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static String currentSessionKey() {
        Context c = HOLDER.get();
        return c == null ? "anonymous" : c.sessionKey();
    }

    public static void sendArtifact(String fileId, String filename) {
        Context ctx = HOLDER.get();
        if (ctx == null || fileId == null || fileId.isBlank()) {
            return;
        }
        try {
            String json = MAPPER.writeValueAsString(Map.of(
                    "fileId", fileId,
                    "filename", filename == null ? "report.pdf" : filename
            ));
            ctx.emitter().send(SseEmitter.event().name("artifact").data(json));
        } catch (IOException e) {
            try {
                ctx.emitter().completeWithError(e);
            } catch (Exception ignored) {
                // ignore
            }
        }
    }
}

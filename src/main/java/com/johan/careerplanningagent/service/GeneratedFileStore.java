package com.johan.careerplanningagent.service;

import com.johan.careerplanningagent.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录工具生成的文件，供下载接口按 fileId + 会话校验后返回文件流。
 */
@Service
public class GeneratedFileStore {

    public record FileMeta(Path absolutePath, String ownerSessionKey, String originalFilename, Instant createdAt) {
    }

    private final ConcurrentHashMap<String, FileMeta> byId = new ConcurrentHashMap<>();

    public String register(Path absolutePath, String ownerSessionKey, String originalFilename) {
        String id = UUID.randomUUID().toString().replace("-", "");
        String owner = ownerSessionKey == null || ownerSessionKey.isBlank() ? "anonymous" : ownerSessionKey;
        byId.put(id, new FileMeta(absolutePath, owner, originalFilename, Instant.now()));
        return id;
    }

    public FileMeta require(String fileId, String requestSessionKey) {
        if (fileId == null || fileId.isBlank()) {
            throw new BusinessException(400, "fileId 不能为空");
        }
        FileMeta meta = byId.get(fileId);
        if (meta == null) {
            throw new BusinessException(404, "文件不存在或已过期");
        }
        String session = requestSessionKey == null || requestSessionKey.isBlank() ? "anonymous" : requestSessionKey;
        if (!meta.ownerSessionKey().equals(session)) {
            throw new BusinessException(403, "无权下载该文件");
        }
        return meta;
    }
}

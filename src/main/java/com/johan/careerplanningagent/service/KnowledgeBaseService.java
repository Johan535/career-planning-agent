package com.johan.careerplanningagent.service;

import com.johan.careerplanningagent.exception.BusinessException;
import com.johan.careerplanningagent.model.KnowledgeFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final VectorStore defaultVectorStore;
    private final VectorStore pgVectorStore;
    private final boolean usePgVector;
    private final Path baseDir = Path.of(System.getProperty("user.dir"), "knowledge-base");

    public KnowledgeBaseService(@Qualifier("carePlanningAgentVectorStore") VectorStore defaultVectorStore,
                                @Autowired(required = false) @Qualifier("pgVectorStore") VectorStore pgVectorStore,
                                @Value("${app.rag.use-pgvector:false}") boolean usePgVector) throws IOException {
        this.defaultVectorStore = defaultVectorStore;
        this.pgVectorStore = pgVectorStore;
        this.usePgVector = usePgVector;
        Files.createDirectories(baseDir);
    }

    public synchronized KnowledgeFileInfo upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上傳文件不能為空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(400, "文件名不合法");
        }
        String safeName = Path.of(originalName).getFileName().toString();
        Path target = baseDir.resolve(Instant.now().toEpochMilli() + "_" + safeName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new KnowledgeFileInfo(
                    target.getFileName().toString(),
                    Files.size(target),
                    Instant.now().toString(),
                    false,
                    "已上傳，請調用 /knowledge/reindex 建立索引"
            );
        } catch (IOException e) {
            throw new BusinessException(500, "知識庫文件上傳失敗");
        }
    }

    public synchronized List<KnowledgeFileInfo> listFiles() {
        if (!Files.exists(baseDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(baseDir)) {
            return stream.filter(Files::isRegularFile)
                    .sorted()
                    .map(path -> {
                        try {
                            return new KnowledgeFileInfo(path.getFileName().toString(), Files.size(path),
                                    Files.getLastModifiedTime(path).toInstant().toString(),
                                    false,
                                    "尚未驗證索引狀態");
                        } catch (IOException e) {
                            return new KnowledgeFileInfo(path.getFileName().toString(), 0, Instant.now().toString(),
                                    false, "讀取文件資訊失敗");
                        }
                    }).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    public synchronized int reindexAll() {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(baseDir)) {
            stream.filter(Files::isRegularFile).forEach(files::add);
        } catch (IOException e) {
            throw new BusinessException(500, "讀取知識庫文件失敗");
        }
        for (Path file : files) {
            tryIndexFile(file);
        }
        return files.size();
    }

    private IndexResult tryIndexFile(Path path) {
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            if (text.isBlank()) {
                log.warn("知識庫文件內容為空，跳過索引: {}", path.getFileName());
                return new IndexResult(false, "內容為空，已跳過索引");
            }
            Document document = new Document(text, Map.of("source", path.getFileName().toString()));
            resolveVectorStore().add(List.of(document));
            return new IndexResult(true, "索引成功");
        } catch (Exception ex) {
            log.warn("知識庫索引失敗: {}", path.getFileName(), ex);
            return new IndexResult(false, "索引失敗: " + ex.getMessage());
        }
    }

    private VectorStore resolveVectorStore() {
        if (usePgVector && pgVectorStore != null) {
            return pgVectorStore;
        }
        return defaultVectorStore;
    }

    private record IndexResult(boolean success, String message) {
    }
}

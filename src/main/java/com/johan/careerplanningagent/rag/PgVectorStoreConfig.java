package com.johan.careerplanningagent.rag;


import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

//PG向量数据库配置
@Configuration
@ConditionalOnProperty(name = "app.rag.pgvector-enabled", havingValue = "true")
public class PgVectorStoreConfig {
    private static final Logger log = LoggerFactory.getLogger(PgVectorStoreConfig.class);

    @Resource
    private CarePlanningAgentDocumentLoader carePlanningAgentDocumentLoader;
    
    @Value("${spring.ai.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Bean
    public VectorStore pgVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536) //意思是模型输出的维度
                .distanceType(COSINE_DISTANCE) //距离计算方式
                .indexType(HNSW) //索引类型
                .initializeSchema(true) //初始化数据库
                .schemaName("public")  //索引表名
                .vectorTableName("vector_store") //索引表名
                .maxDocumentBatchSize(100) //减小批量大小，避免网络超时（从10000改为100）
                .build(); //创建向量数据库

        log.info("PgVectorStore Bean创建完成，文档将在应用启动后异步加载");
        return vectorStore; //返回向量数据库
    }
    
    /**
     * 应用启动后异步加载文档到PgVectorStore
     * 这样可以避免在启动过程中因网络请求被中断而导致失败
     */
    @Bean
    public ApplicationRunner loadDocumentsToPgVector(@Qualifier("pgVectorStore") VectorStore vectorStore) {
        return args -> {
            if (!"test-key".equals(dashscopeApiKey) && dashscopeApiKey != null && !dashscopeApiKey.isEmpty()) {
                try {
                    log.info("开始异步加载文档到PgVectorStore...");
                    List<Document> documents = carePlanningAgentDocumentLoader.loadMarkDowns();
                    
                    if (documents != null && !documents.isEmpty()) {
                        log.info("成功加载 {} 个文档，开始分批添加到向量数据库...", documents.size());
                        // 分批添加，避免一次性请求过大
                        int batchSize = 10;
                        for (int i = 0; i < documents.size(); i += batchSize) {
                            int end = Math.min(i + batchSize, documents.size());
                            List<Document> batch = documents.subList(i, end);
                            log.info("正在处理第 {} 批，共 {} 个文档...", (i / batchSize + 1), batch.size());
                            vectorStore.add(batch);
                        }
                        log.info("PgVectorStore文档加载完成！");
                    } else {
                        log.warn("未加载到任何文档");
                    }
                } catch (Exception e) {
                    log.warn(" PgVectorStore文档加载失败，但不影响应用运行: {}", e.getMessage());
                    log.debug("详细错误信息: ", e);
                }
            } else {
                log.warn("未配置有效 DashScope API Key，跳过文档加载");
            }
        };
    }
}

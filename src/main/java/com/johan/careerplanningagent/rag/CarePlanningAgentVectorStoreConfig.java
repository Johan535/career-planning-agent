package com.johan.careerplanningagent.rag;


import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;

/*
* AI职业规划师向量存储配置（初始化基于内存 的向量数据库Bean）
* */
@Configuration
public class CarePlanningAgentVectorStoreConfig {
    private static final Logger log = LoggerFactory.getLogger(CarePlanningAgentVectorStoreConfig.class);

    // 文档加载器
    @Resource
    private CarePlanningAgentDocumentLoader carePlanningAgentDocumentLoader;

    // 向量模型
    @Resource
    private  MyKeywordEnricher myKeywordEnricher;

    @Value("${spring.ai.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Bean
    VectorStore carePlanningAgentVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        // 创建基于内存的向量数据库
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        if (!StringUtils.hasText(dashscopeApiKey) || "test-key".equals(dashscopeApiKey)) {
            log.warn("未配置有效 DashScope API Key，向量庫將以空庫模式啟動");
            return simpleVectorStore;
        }
        
        // 不在启动时加载文档，改为异步加载
        log.info("向量库Bean创建完成，文档将在应用启动后异步加载");
        return simpleVectorStore; // 返回向量数据库
    }
    
    /**
     * 应用启动后异步加载文档到向量库
     * 这样可以避免在启动过程中因网络请求被中断而导致失败
     */
    @Bean
    public ApplicationRunner loadDocumentsAsync(@Qualifier("carePlanningAgentVectorStore") VectorStore vectorStore) {
        return args -> {
            if (!StringUtils.hasText(dashscopeApiKey) || "test-key".equals(dashscopeApiKey)) {
                log.warn("未配置有效 DashScope API Key，跳过文档加载");
                return;
            }
            
            try {
                log.info("开始异步加载文档到向量库...");
                List<Document> documents = carePlanningAgentDocumentLoader.loadMarkDowns();
                
                if (documents == null || documents.isEmpty()) {
                    log.warn("未加载到任何文档");
                    return;
                }
                
                log.info("成功加载 {} 个文档，开始进行关键词增强...", documents.size());
                List<Document> enrichDocuments = myKeywordEnricher.enrichDocuments(documents);
                
                if (enrichDocuments != null && !enrichDocuments.isEmpty()) {
                    log.info("开始将 {} 个文档添加到向量库...", enrichDocuments.size());
                    vectorStore.add(enrichDocuments);
                    log.info("✅ 向量库文档加载完成！");
                }
            } catch (Exception ex) {
                log.warn("⚠️ 向量库文档加载失败，但不影响应用运行: {}", ex.getMessage());
                log.debug("详细错误信息: ", ex);
            }
        };
    }
}

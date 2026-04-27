package com.johan.careerplanningagent.rag;


import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
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
        List<Document> documents = carePlanningAgentDocumentLoader.loadMarkDowns(); // 加载文档
        //基于AI的自主增强(自动补充关键词元信息)
        List<Document> enrichDocuments = myKeywordEnricher.enrichDocuments(documents); // 添加关键词
        try {
            simpleVectorStore.add(enrichDocuments); // 添加文档
        } catch (Exception ex) {
            log.warn("向量庫初始化失敗，回退為空庫: {}", ex.getMessage());
        }
        return simpleVectorStore; // 返回向量数据库
    }
}

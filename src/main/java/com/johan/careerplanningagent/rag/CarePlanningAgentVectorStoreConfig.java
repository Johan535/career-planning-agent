package com.johan.careerplanningagent.rag;


import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/*
* AI职业规划师向量存储配置（初始化基于内存 的向量数据库Bean）
* */
@Configuration
public class CarePlanningAgentVectorStoreConfig {

    @Resource
    private CarePlanningAgentDocumentLoader carePlanningAgentDocumentLoader;

    @Resource
    private  MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore carePlanningAgentVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        // 创建基于内存的向量数据库
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        List<Document> documents = carePlanningAgentDocumentLoader.loadMarkDowns(); // 加载文档
        //基于AI的自主增强(自动补充关键词元信息)
        List<Document> enrichDocuments = myKeywordEnricher.enrichDocuments(documents); // 添加关键词
        simpleVectorStore.add(enrichDocuments); // 添加文档
        return simpleVectorStore; // 返回向量数据库
    }
}

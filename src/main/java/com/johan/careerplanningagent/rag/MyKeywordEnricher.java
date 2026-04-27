package com.johan.careerplanningagent.rag;


import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于AI 的文档辕信息增强器（为文档补充元信息）
 */
@Component
public class MyKeywordEnricher {
    private static final Logger log = LoggerFactory.getLogger(MyKeywordEnricher.class);

    @Resource
    private ChatModel chatModel; //ChatModel作用是获取模型

    public List<Document> enrichDocuments(List<Document> documents) {
        try {
            // 创建一个关键词信息增强器
            KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(chatModel, 5);
            return keywordMetadataEnricher.apply(documents); //apply作用是添加关键词
        } catch (Exception ex) {
            log.warn("关键词增强失败，回退为原始文档: {}", ex.getMessage());
            return documents;
        }
    }
}

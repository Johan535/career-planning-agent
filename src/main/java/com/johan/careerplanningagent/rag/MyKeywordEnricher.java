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
        if (documents == null || documents.isEmpty()) {
            log.debug("文档列表为空，跳过关键词增强");
            return documents;
        }
        
        try {
            log.info("开始对 {} 个文档进行关键词增强...", documents.size());
            // 创建一个关键词信息增强器
            KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(chatModel, 5);
            List<Document> enrichedDocs = keywordMetadataEnricher.apply(documents); //apply作用是添加关键词
            log.info("关键词增强完成，成功处理 {} 个文档", enrichedDocs.size());
            return enrichedDocs;
        } catch (Exception ex) {
            log.warn("关键词增强失败，回退为原始文档: {}", ex.getMessage());
            log.debug("详细错误信息: ", ex);
            return documents; // 返回原始文档，不中断应用启动
        }
    }
}

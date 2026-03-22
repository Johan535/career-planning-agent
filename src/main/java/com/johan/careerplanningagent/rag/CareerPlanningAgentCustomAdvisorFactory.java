package com.johan.careerplanningagent.rag;

import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 创建一个自动逸的RAG检索增强顾问的工厂
 */
public class CareerPlanningAgentCustomAdvisorFactory {

    /**
     * 创建一个自动逸的RAG检索增强顾问
     * @param vectorStore 向量数据库
     * @param status 状态
     * @return
     */
    public static Advisor createCareerPlanningAgentCustom(VectorStore vectorStore,String status) {
        //创建一个过滤表达式(过滤特定状态的文档)
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("status", status)
                .build();

        // 创建一个基于向量数据库的文档检索器
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression) // 添加过滤条件
                .similarityThreshold(0.5) // 设置相似度阈值
                .topK(3) // 设置返回的文档数量
                .build();

        // 创建一个自动逸的RAG检索增强顾问
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever) //文档检索器
                //.queryAugmenter() //文档增强器
                .build();
    }
}

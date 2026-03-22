package com.johan.careerplanningagent.rag;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;


/*
* 查询重写器: 对原始查询进行重写，以获取更好的结果
* */
@Component
public class QueryRewriter {

    // 查询重写器核心对象（封装了大模型重写逻辑）
    private final QueryTransformer queryTransformer;

    // 构造函数: 创建查询重写器
    public QueryRewriter(ChatModel dashscopeChatMode) {
        // 1. 基于阿里云大模型创建 ChatClient 构建器
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatMode);
        // 2. 构建 RewriteQueryTransformer（查询重写器）
        queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder) // 绑定大模型客户端
                .build(); // 完成初始化
    }

    public String doQueryRewrite(String prompt) {
        // 1. 将原始查询字符串封装为 RAG 标准的 Query 对象
        Query query = new Query(prompt);
        // 2. 执行查询重写：调用 QueryTransformer 的 transform 方法
        Query transform = queryTransformer.transform(query);
        // 3. 返回重写后的查询文本
        return transform.text();
    }
}

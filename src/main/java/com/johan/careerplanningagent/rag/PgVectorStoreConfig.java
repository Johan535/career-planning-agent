package com.johan.careerplanningagent.rag;


import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

//PG向量数据库配置
@Configuration
public class PgVectorStoreConfig {

    @Resource
    private CarePlanningAgentDocumentLoader carePlanningAgentDocumentLoader;

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

        try {
            //从classpath：document/*.md中加载所有markdown文档
            List<Document> documents = carePlanningAgentDocumentLoader.loadMarkDowns();
            if (documents != null && !documents.isEmpty()) {
                System.out.println("开始加载 " + documents.size() + " 个文档到向量数据库...");
                // 分批添加，避免一次性请求过大
                int batchSize = 10;
                for (int i = 0; i < documents.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, documents.size());
                    List<Document> batch = documents.subList(i, end);
                    System.out.println("正在处理第 " + (i / batchSize + 1) + " 批，共 " + batch.size() + " 个文档...");
                    vectorStore.add(batch);
                }
                System.out.println("文档加载完成！");
            }
        } catch (Exception e) {
            System.err.println("加载文档到向量数据库时出错: " + e.getMessage());
            e.printStackTrace();
            // 不抛出异常，允许应用继续启动
        }
        
        return vectorStore; //返回向量数据库

    }
}

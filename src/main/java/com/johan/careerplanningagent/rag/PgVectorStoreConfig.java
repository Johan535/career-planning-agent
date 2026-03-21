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


/**
 * PG向量数据库配置
 */
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
                .maxDocumentBatchSize(10000) //批量插入的文档数量
                .build(); //创建向量数据库

        //从classpath：markdowns/*.md中加载所有markdown文档
        List<Document> documents = carePlanningAgentDocumentLoader.loadMarkDowns();
        vectorStore.add(documents); //将文档添加到向量数据库中
        return vectorStore; //返回向量数据库

    }
}

package com.johan.careerplanningagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class PgVectorVectorStoreConfigTest {

    @Resource(name = "pgVectorStore")
    private VectorStore pgVectorVectorStore;

    @Test
    void test() {
        System.out.println("VectorStore 类型：" + pgVectorVectorStore.getClass().getName());

        List<Document> documents = List.of(
                new Document("新人如何快速在职场上转型", Map.of("meta1", "meta1")),
                new Document( "如何快速在existing project中加入新的功能", Map.of("meta2", "meta2")),
                new Document(  "wjh比较帅", Map.of("meta2", "meta2")));

        //添加文档
        pgVectorVectorStore.add(documents);

        //相似度查询
        List<Document> results = pgVectorVectorStore.similaritySearch(SearchRequest.builder().query("怎么在职场上快速转型啊").topK(5).build());
        Assertions.assertNotNull(results);
    }
}

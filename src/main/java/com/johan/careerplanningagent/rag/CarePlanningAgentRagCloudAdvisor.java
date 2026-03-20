package com.johan.careerplanningagent.rag;


import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 自定义基于阿里云知识库服务的RAG 增强顾问
 */
@Configuration
public class CarePlanningAgentRagCloudAdvisor {

    //作用是获取DashScope的API Key
    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    private static final String KNOWLEDGE_INDEX = "职业规划师";

    //创建RAG云Advisor
    @Bean
    public Advisor carePlanningAgentCloudAdvisor() {
        DashScopeApi dashScopeApi = new DashScopeApi.Builder()
                .apiKey(dashScopeApiKey)
                .build(); //创建基于DashScope的API对象
        DashScopeDocumentRetriever dashScopeDocumentRetriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName(KNOWLEDGE_INDEX)
                        .build()); //创建基于DashScope的文档检索对象
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(dashScopeDocumentRetriever)
                .build();
    }
}

package com.johan.careerplanningagent.config;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * HTTP客户端配置类
 * 解决Request was interrupted的问题
 */

/**
 * HTTP客户端配置类
 * 解决Request was interrupted的问题
 */
@Configuration
@ConditionalOnProperty(name = "external-service.enabled", havingValue = "true", matchIfMissing = false) // 启用外部服务时才启用
public class HttpClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        // 创建配置了超时的JDK HttpClient
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        // 创建请求工厂并设置超时
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .requestFactory(requestFactory);
    }
}

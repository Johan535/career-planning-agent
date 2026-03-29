package com.johan.careerplanningagent.config;


import org.springframework.context.annotation.Configuration;
import  org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
* 全域跨域配置
* */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许所有请求
        registry.addMapping("/**")
                // 允许携带cookie
                .allowCredentials(true)
                // 允许所有域名访问
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .exposedHeaders("*")
                .maxAge(3600);
    }

}

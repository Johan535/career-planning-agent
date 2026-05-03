package com.johan.careerplanningagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiSecurityInterceptor apiSecurityInterceptor;

    public WebConfig(ApiSecurityInterceptor apiSecurityInterceptor) {
        this.apiSecurityInterceptor = apiSecurityInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiSecurityInterceptor)
                .addPathPatterns("/ai/**");
    }
}

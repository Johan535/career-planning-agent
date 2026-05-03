package com.johan.careerplanningagent.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiSecurityInterceptor implements HandlerInterceptor {

    @Value("${app.security.enabled:false}")
    private boolean enabled;

    @Value("${app.security.api-key:}")
    private String configuredApiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled) {
            return true;
        }

        String requestApiKey = request.getHeader("X-API-KEY");
        if (requestApiKey == null || requestApiKey.isBlank()) {
            String q = request.getParameter("apiKey");
            if (q != null && !q.isBlank()) {
                requestApiKey = q;
            }
        }
        if (configuredApiKey != null && configuredApiKey.equals(requestApiKey)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未授權訪問\",\"data\":null}");
        return false;
    }
}

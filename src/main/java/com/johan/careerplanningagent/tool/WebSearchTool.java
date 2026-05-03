package com.johan.careerplanningagent.tool;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网页搜索工具
 */
public class WebSearchTool {

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    private final String apiKey;

    public WebSearchTool(@Value("${search-api.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(@ToolParam(description = "Search query keyword") String query) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            if (organicResults == null || organicResults.isEmpty()) {
                return "未搜索到可用结果，请基于已有知识继续完成任务。原始响应: " + response;
            }

            int limit = Math.min(5, organicResults.size());
            List<Object> objects = organicResults.subList(0, limit);
            return objects.stream()
                    .map(obj -> obj instanceof JSONObject json ? json.toString() : String.valueOf(obj))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "搜索工具调用失败，请基于已有知识继续完成任务。错误信息: " + e.getMessage();
        }
    }
}

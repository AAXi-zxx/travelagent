package com.travel.ai.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网页搜索工具
 */
public class WebSearchTool {

    // SearchAPI 的搜索接口地址
    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            // 取出返回结果的前 5 条
            JSONObject jsonObject = JSONUtil.parseObj(response);
            // 提取 organic_results 部分（为空说明 key 无效或请求失败，返回接口原始信息便于排查）
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            if (organicResults == null || organicResults.isEmpty()) {
                return "搜索接口未返回结果：" + response;
            }
            List<Object> objects = organicResults.subList(0, Math.min(5, organicResults.size()));
            // 输出干净的「序号 + 标题 + 链接 + 摘要」格式，避免把原始 JSON 全量暴露给用户
            StringBuilder sb = new StringBuilder();
            int index = 1;
            for (Object obj : objects) {
                JSONObject item = (JSONObject) obj;
                String title = item.getStr("title", "");
                String link = item.getStr("link", "");
                String snippet = item.getStr("snippet", "");
                // 摘要截断，避免过长
                if (snippet.length() > 120) {
                    snippet = snippet.substring(0, 120) + "...";
                }
                sb.append(index++).append(". ").append(title).append("\n")
                  .append("   链接：").append(link).append("\n");
                if (!snippet.isBlank()) {
                    sb.append("   摘要：").append(snippet).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error searching Baidu: " + e.getMessage();
        }
    }
}

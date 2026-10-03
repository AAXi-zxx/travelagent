package com.travel.ai.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图片搜索工具（调用 Pexels 图片搜索 API）
 * <p>
 * 作为统一智能体可调用的内置工具，让大模型在用户提出「找图片」需求时自动搜索并返回图片链接。
 */
public class ImageSearchTool {

    // Pexels API 密钥：通过环境变量 PEXELS_API_KEY 注入，不要硬编码提交
    private final String apiKey;

    // Pexels 常规搜索接口
    private static final String API_URL = "https://api.pexels.com/v1/search";

    // 本轮（同一线程内）工具返回的图片 URL，供上层在模型偷懒不贴链接时程序化兜底
    private static final ThreadLocal<List<String>> LAST_RESULTS = new ThreadLocal<>();

    public ImageSearchTool() {
        String key = System.getenv("PEXELS_API_KEY");
        this.apiKey = key != null ? key : "";
    }

    @Tool(description = "Search images from Pexels and return image URLs as Markdown image syntax, one per line. You MUST copy all returned lines into your reply verbatim.")
    public String searchImage(@ToolParam(description = "Search query keyword") String query) {
        try {
            List<String> urls = searchMediumImages(query);
            LAST_RESULTS.set(urls);
            // 直接返回 Markdown 图片语法，模型只需原样复述，避免「只说找到却不贴链接」
            return urls.stream()
                    .map(url -> "![图片](" + url + ")")
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "Error searching image: " + e.getMessage();
        }
    }

    /**
     * 取出并清空本轮（当前请求线程）工具返回的图片 URL。
     * Spring AI 的同步工具执行与调用方在同一线程，ThreadLocal 可靠。
     */
    public static List<String> consumeLastResults() {
        List<String> results = LAST_RESULTS.get();
        LAST_RESULTS.remove();
        return results != null ? results : java.util.List.of();
    }

    /**
     * 搜索中等尺寸的图片列表
     *
     * @param query 搜索关键词
     * @return 图片 URL 列表
     */
    public List<String> searchMediumImages(String query) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", apiKey);

        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("per_page", 5); // 限制返回数量，避免一次返回过多图片

        String response = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
                .execute()
                .body();

        return JSONUtil.parseObj(response)
                .getJSONArray("photos")
                .stream()
                .map(photoObj -> (JSONObject) photoObj)
                .map(photoObj -> photoObj.getJSONObject("src"))
                .map(photo -> photo.getStr("medium"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}

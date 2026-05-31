package com.pullcat.service.llm;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullcat.model.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JSON 输出解析工具类，用于从 LLM 返回的文本中提取并解析问题列表。
 */
public final class JsonOutputParser {

    private static final Logger log = LoggerFactory.getLogger(JsonOutputParser.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonOutputParser() {
    }

    /**
     * 从 LLM 响应文本中解析问题列表。
     *
     * @param response LLM 返回的原始响应文本
     * @return 解析后的问题列表，解析失败时返回空列表
     */
    public static List<Issue> parseIssues(String response) {
        try {
            String json = extractJson(response);
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> issuesArray = (List<Map<String, Object>>) map.get("issues");
            if (issuesArray == null) {
                return Collections.emptyList();
            }
            return issuesArray.stream()
                    .map(JsonOutputParser::mapToIssue)
                    .toList();
        } catch (Exception e) {
            log.warn("解析 LLM 响应中的问题列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 从 LLM 响应文本中提取 JSON 部分，去除 markdown 代码块标记或定位首对大括号。
     *
     * @param response 原始响应文本
     * @return 提取的 JSON 字符串
     */
    public static String extractJson(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }
        String trimmed = response.trim();

        // 去除 ```json ... ``` 标记
        if (trimmed.startsWith("```json") && trimmed.endsWith("```")) {
            return trimmed.substring(7, trimmed.length() - 3).trim();
        }
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            return trimmed.substring(3, trimmed.length() - 3).trim();
        }

        // 定位第一个 { 和最后一个 }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }

    /**
     * 将 Map 转换为 Issue 对象。
     */
    private static Issue mapToIssue(Map<String, Object> map) {
        Issue issue = new Issue();
        String severityStr = (String) map.get("severity");
        if (severityStr != null) {
            try {
                issue.setSeverity(Issue.Severity.valueOf(severityStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                issue.setSeverity(Issue.Severity.INFO);
            }
        }
        issue.setFile((String) map.get("file"));
        Object line = map.get("line");
        if (line instanceof Number) {
            issue.setLine(((Number) line).intValue());
        }
        issue.setTitle((String) map.get("title"));
        issue.setDescription((String) map.get("description"));
        issue.setSuggestion((String) map.get("suggestion"));
        Object confidence = map.get("confidence");
        if (confidence instanceof Number) {
            issue.setConfidence(((Number) confidence).doubleValue());
        }
        issue.setSuggestionCode((String) map.get("suggestionCode"));
        return issue;
    }
}

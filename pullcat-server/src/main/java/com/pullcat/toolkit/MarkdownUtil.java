package com.pullcat.toolkit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullcat.dto.resp.IssueRespDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Markdown 与审查内容格式化工具
 */
public final class MarkdownUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(MarkdownUtil.class);

    private MarkdownUtil() {
    }

    /**
     * 从 LLM 返回的 JSON 内容中提取 summary 字段文本
     */
    public static String extractSummaryText(String content) {
        try {
            String json = JsonOutputParser.extractJson(content);
            var node = objectMapper.readTree(json);
            return node.has("summary") ? node.get("summary").asText("") : content;
        } catch (Exception e) {
            log.warn("提取 summary 文本失败: {}", e.getMessage());
            return content;
        }
    }

    /**
     * 构建单条 issue 的 inline comment 修复建议块
     */
    public static String buildSuggestionBlock(IssueRespDTO issue) {
        StringBuilder sb = new StringBuilder();
        sb.append("**[").append(issue.getSeverity()).append("] ").append(issue.getTitle()).append("**\n\n");
        sb.append(issue.getDescription()).append("\n\n");
        sb.append("```suggestion\n");
        sb.append(issue.getSuggestionCode());
        sb.append("\n```\n");
        return sb.toString();
    }
}

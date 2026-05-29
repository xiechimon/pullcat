package com.pullcat.service.analysis;

import com.pullcat.model.Issue;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 结果聚合器，合并多块分析结果，按 file:line:title 去重并按严重程度排序。
 */
@Component
public class ResultAggregator {

    /**
     * 合并多个分块的分析结果。
     */
    public List<Issue> mergeIssues(List<List<Issue>> chunkResults) {
        List<Issue> merged = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (List<Issue> chunk : chunkResults) {
            if (chunk == null) continue;
            for (Issue issue : chunk) {
                String key = buildDedupKey(issue);
                if (seen.add(key)) {
                    merged.add(issue);
                }
            }
        }

        merged.sort(this::compareBySeverity);
        return merged;
    }

    private int compareBySeverity(Issue a, Issue b) {
        return severityWeight(b.getSeverity()) - severityWeight(a.getSeverity());
    }

    private int severityWeight(Issue.Severity severity) {
        if (severity == null) return 0;
        return switch (severity) {
            case CRITICAL -> 5;
            case HIGH -> 4;
            case MEDIUM -> 3;
            case LOW -> 2;
            case INFO -> 1;
        };
    }

    private String buildDedupKey(Issue issue) {
        String file = issue.getFile() != null ? issue.getFile() : "";
        String line = issue.getLine() != null ? String.valueOf(issue.getLine()) : "";
        String title = issue.getTitle() != null ? issue.getTitle() : "";
        String prefix = title.length() > 50 ? title.substring(0, 50) : title;
        return file + ":" + line + ":" + prefix;
    }
}

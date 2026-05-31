package com.pullcat.service.analysis;

import com.pullcat.model.AnalysisResult;
import com.pullcat.model.Issue;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ResultAggregator {

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

    public List<Issue> mergeResults(List<AnalysisResult> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, DedupEntry> dedupMap = new LinkedHashMap<>();

        for (AnalysisResult result : results) {
            if (result == null || result.getIssues() == null) continue;

            for (Issue issue : result.getIssues()) {
                String key = buildDedupKey(issue);
                DedupEntry existing = dedupMap.get(key);

                if (existing == null) {
                    dedupMap.put(key, new DedupEntry(issue, result.getType().name()));
                } else {
                    mergeInto(existing, issue, result.getType().name());
                }
            }
        }

        List<Issue> merged = new ArrayList<>();
        for (DedupEntry entry : dedupMap.values()) {
            Issue resolved = resolveMerged(entry);
            merged.add(resolved);
        }

        merged.sort(this::compareBySeverity);
        return merged;
    }

    private void mergeInto(DedupEntry entry, Issue incoming, String dimensionName) {
        entry.dimensions.add(dimensionName);

        if (entry.issue.getSeverity() == null || incoming.getSeverity() != null
                && severityWeight(incoming.getSeverity()) > severityWeight(entry.issue.getSeverity())) {
            entry.issue.setSeverity(incoming.getSeverity());
        }

        if (incoming.getConfidence() != null
                && (entry.issue.getConfidence() == null
                || incoming.getConfidence() > entry.issue.getConfidence())) {
            entry.issue.setConfidence(incoming.getConfidence());
        }

        if (incoming.getDescription() != null
                && (entry.issue.getDescription() == null
                || incoming.getDescription().length() > entry.issue.getDescription().length())) {
            entry.issue.setDescription(incoming.getDescription());
        }

        if (incoming.getSuggestion() != null && entry.issue.getSuggestion() == null) {
            entry.issue.setSuggestion(incoming.getSuggestion());
        }

        if (incoming.getSuggestionCode() != null && entry.issue.getSuggestionCode() == null) {
            entry.issue.setSuggestionCode(incoming.getSuggestionCode());
        }
    }

    private Issue resolveMerged(DedupEntry entry) {
        if (entry.dimensions.size() <= 1) {
            entry.issue.getSourceDimensions().add(entry.dimensions.iterator().next());
            return entry.issue;
        }

        entry.issue.setSourceDimensions(new ArrayList<>(entry.dimensions));

        String dimensionTag = buildDimensionTag(new ArrayList<>(entry.dimensions));
        if (dimensionTag != null && !entry.issue.getTitle().contains(dimensionTag)) {
            entry.issue.setTitle(entry.issue.getTitle() + " " + dimensionTag);
        }

        return entry.issue;
    }

    private String buildDimensionTag(List<String> dimensions) {
        Map<String, String> shortNames = Map.of(
                "RISK", "风险",
                "QUALITY", "质量",
                "CONSISTENCY", "一致性",
                "TESTING", "测试",
                "SUMMARY", "摘要"
        );

        List<String> names = dimensions.stream()
                .map(d -> shortNames.getOrDefault(d, d))
                .distinct()
                .toList();

        if (names.size() >= 2) {
            return "[" + String.join("+", names) + "双重发现]";
        }
        return null;
    }

    private int compareBySeverity(Issue a, Issue b) {
        return severityWeight(b.getSeverity()) - severityWeight(a.getSeverity());
    }

    int severityWeight(Issue.Severity severity) {
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
        String title = issue.getTitle() != null ? issue.getTitle().trim().toLowerCase() : "";
        String prefix = title.length() > 50 ? title.substring(0, 50) : title;
        return file + ":" + line + ":" + prefix;
    }

    private static class DedupEntry {
        final Issue issue;
        final Set<String> dimensions = new LinkedHashSet<>();

        DedupEntry(Issue issue, String dimension) {
            this.issue = issue;
            this.dimensions.add(dimension);
        }
    }
}

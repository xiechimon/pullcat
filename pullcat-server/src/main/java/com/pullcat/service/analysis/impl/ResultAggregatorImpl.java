package com.pullcat.service.analysis.impl;

import com.pullcat.common.enums.Severity;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.service.analysis.ResultAggregator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ResultAggregatorImpl implements ResultAggregator {

    @Override
    public List<IssueRespDTO> mergeIssues(List<List<IssueRespDTO>> chunkResults) {
        List<IssueRespDTO> merged = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (List<IssueRespDTO> chunk : chunkResults) {
            if (chunk == null) {
                continue;
            }
            for (IssueRespDTO issue : chunk) {
                String key = buildDedupKey(issue);
                if (seen.add(key)) {
                    merged.add(issue);
                }
            }
        }

        merged.sort(this::compareBySeverity);
        return merged;
    }

    @Override
    public List<IssueRespDTO> mergeResults(List<AnalysisResultRespDTO> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, DedupEntry> dedupMap = new LinkedHashMap<>();

        for (AnalysisResultRespDTO result : results) {
            if (result == null || result.getIssues() == null) {
                continue;
            }

            for (IssueRespDTO issue : result.getIssues()) {
                String key = buildDedupKey(issue);
                DedupEntry existing = dedupMap.get(key);

                if (existing == null) {
                    dedupMap.put(key, new DedupEntry(issue, result.getType().name()));
                } else {
                    mergeInto(existing, issue, result.getType().name());
                }
            }
        }

        List<IssueRespDTO> merged = new ArrayList<>();
        for (DedupEntry entry : dedupMap.values()) {
            merged.add(resolveMerged(entry));
        }

        merged.sort(this::compareBySeverity);
        return merged;
    }

    private void mergeInto(DedupEntry entry, IssueRespDTO incoming, String dimensionName) {
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

    private IssueRespDTO resolveMerged(DedupEntry entry) {
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
                .map(dimension -> shortNames.getOrDefault(dimension, dimension))
                .distinct()
                .toList();

        if (names.size() >= 2) {
            return "[" + String.join("+", names) + "双重发现]";
        }
        return null;
    }

    private int compareBySeverity(IssueRespDTO a, IssueRespDTO b) {
        return severityWeight(b.getSeverity()) - severityWeight(a.getSeverity());
    }

    int severityWeight(Severity severity) {
        if (severity == null) {
            return 0;
        }
        return switch (severity) {
            case CRITICAL -> 5;
            case HIGH -> 4;
            case MEDIUM -> 3;
            case LOW -> 2;
            case INFO -> 1;
        };
    }

    private String buildDedupKey(IssueRespDTO issue) {
        String file = issue.getFile() != null ? issue.getFile() : "";
        String line = issue.getLine() != null ? String.valueOf(issue.getLine()) : "";
        String title = issue.getTitle() != null ? issue.getTitle().trim().toLowerCase() : "";
        String prefix = title.length() > 50 ? title.substring(0, 50) : title;
        return file + ":" + line + ":" + prefix;
    }

    private static class DedupEntry {
        final IssueRespDTO issue;
        final Set<String> dimensions = new LinkedHashSet<>();

        private DedupEntry(IssueRespDTO issue, String dimension) {
            this.issue = issue;
            this.dimensions.add(dimension);
        }
    }
}

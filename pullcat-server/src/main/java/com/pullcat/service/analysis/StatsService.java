package com.pullcat.service.analysis;

import com.pullcat.model.AnalysisResult;
import com.pullcat.model.Issue;
import com.pullcat.model.ReviewSession;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private final ReviewRepository reviewRepository;

    public StatsService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public Map<String, Object> getOverview() {
        List<ReviewSession> all = reviewRepository.findAllReviews();

        int totalReviews = all.size();
        int totalIssues = 0;
        Map<String, Integer> severityCounts = new LinkedHashMap<>();
        severityCounts.put("CRITICAL", 0);
        severityCounts.put("HIGH", 0);
        severityCounts.put("MEDIUM", 0);
        severityCounts.put("LOW", 0);
        severityCounts.put("INFO", 0);

        Map<String, Integer> issueTypeCounts = new HashMap<>();
        java.util.Set<String> repos = new java.util.HashSet<>();

        for (ReviewSession session : all) {
            if (session.getRepositoryFullName() != null) {
                repos.add(session.getRepositoryFullName());
            }
            for (AnalysisResult result : session.getAnalyses().values()) {
                if (result.getIssues() != null) {
                    totalIssues += result.getIssues().size();
                    for (Issue issue : result.getIssues()) {
                        if (issue.getSeverity() != null) {
                            severityCounts.merge(issue.getSeverity().name(), 1, Integer::sum);
                        }
                        if (issue.getTitle() != null) {
                            String key = truncateTitle(issue.getTitle());
                            issueTypeCounts.merge(key, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalReviews", totalReviews);
        overview.put("totalIssues", totalIssues);
        overview.put("repoCount", repos.size());
        overview.put("avgIssuesPerReview", totalReviews > 0 ? (double) totalIssues / totalReviews : 0);
        overview.put("severityDistribution", severityCounts);

        List<Map.Entry<String, Integer>> topTypes = issueTypeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .toList();
        overview.put("commonIssueTypes", topTypes.stream()
                .map(e -> Map.of("type", e.getKey(), "count", e.getValue()))
                .toList());

        return overview;
    }

    public Map<String, Object> getRepoStats(String owner, String repo) {
        String fullName = owner + "/" + repo;
        List<ReviewSession> repoReviews = reviewRepository.findByRepo(fullName, 0, Integer.MAX_VALUE);

        int totalReviews = repoReviews.size();
        int totalIssues = 0;
        Map<String, Integer> severityCounts = new LinkedHashMap<>();
        severityCounts.put("CRITICAL", 0);
        severityCounts.put("HIGH", 0);
        severityCounts.put("MEDIUM", 0);
        severityCounts.put("LOW", 0);
        severityCounts.put("INFO", 0);

        for (ReviewSession session : repoReviews) {
            for (AnalysisResult result : session.getAnalyses().values()) {
                if (result.getIssues() != null) {
                    totalIssues += result.getIssues().size();
                    for (Issue issue : result.getIssues()) {
                        if (issue.getSeverity() != null) {
                            severityCounts.merge(issue.getSeverity().name(), 1, Integer::sum);
                        }
                    }
                }
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalReviews", totalReviews);
        stats.put("totalIssues", totalIssues);
        stats.put("avgIssuesPerReview", totalReviews > 0 ? (double) totalIssues / totalReviews : 0);
        stats.put("severityDistribution", severityCounts);
        stats.put("repoFullName", fullName);

        return stats;
    }

    private String truncateTitle(String title) {
        return title.length() > 50 ? title.substring(0, 50) : title;
    }
}

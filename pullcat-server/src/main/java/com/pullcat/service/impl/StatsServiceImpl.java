package com.pullcat.service.impl;

import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.CommonIssueTypeRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.dto.resp.RepoStatsRespDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.dto.resp.StatsOverviewRespDTO;
import com.pullcat.service.StatsService;
import com.pullcat.service.analysis.ReviewSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final ReviewSessionService reviewSessionService;

    @Override
    public StatsOverviewRespDTO getOverview() {
        List<ReviewSessionRespDTO> all = reviewSessionService.findAllReviews();

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

        for (ReviewSessionRespDTO session : all) {
            if (session.getRepositoryFullName() != null) {
                repos.add(session.getRepositoryFullName());
            }
            for (AnalysisResultRespDTO result : session.getAnalyses().values()) {
                if (result.getIssues() != null) {
                    totalIssues += result.getIssues().size();
                    for (IssueRespDTO issue : result.getIssues()) {
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

        List<Map.Entry<String, Integer>> topTypes = issueTypeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .toList();
        StatsOverviewRespDTO overview = new StatsOverviewRespDTO();
        overview.setTotalReviews(totalReviews);
        overview.setTotalIssues(totalIssues);
        overview.setRepoCount(repos.size());
        overview.setAvgIssuesPerReview(totalReviews > 0 ? (double) totalIssues / totalReviews : 0);
        overview.setSeverityDistribution(severityCounts);
        overview.setCommonIssueTypes(topTypes.stream()
                .map(e -> new CommonIssueTypeRespDTO(e.getKey(), e.getValue()))
                .toList());
        return overview;
    }

    @Override
    public RepoStatsRespDTO getRepoStats(String owner, String repo) {
        String fullName = owner + "/" + repo;
        List<ReviewSessionRespDTO> repoReviews = reviewSessionService.findByRepo(fullName, 0, Integer.MAX_VALUE);

        int totalReviews = repoReviews.size();
        int totalIssues = 0;
        Map<String, Integer> severityCounts = new LinkedHashMap<>();
        severityCounts.put("CRITICAL", 0);
        severityCounts.put("HIGH", 0);
        severityCounts.put("MEDIUM", 0);
        severityCounts.put("LOW", 0);
        severityCounts.put("INFO", 0);

        for (ReviewSessionRespDTO session : repoReviews) {
            for (AnalysisResultRespDTO result : session.getAnalyses().values()) {
                if (result.getIssues() != null) {
                    totalIssues += result.getIssues().size();
                    for (IssueRespDTO issue : result.getIssues()) {
                        if (issue.getSeverity() != null) {
                            severityCounts.merge(issue.getSeverity().name(), 1, Integer::sum);
                        }
                    }
                }
            }
        }

        RepoStatsRespDTO stats = new RepoStatsRespDTO();
        stats.setTotalReviews(totalReviews);
        stats.setTotalIssues(totalIssues);
        stats.setAvgIssuesPerReview(totalReviews > 0 ? (double) totalIssues / totalReviews : 0);
        stats.setSeverityDistribution(severityCounts);
        stats.setRepoFullName(fullName);
        return stats;
    }

    private String truncateTitle(String title) {
        return title.length() > 50 ? title.substring(0, 50) : title;
    }
}

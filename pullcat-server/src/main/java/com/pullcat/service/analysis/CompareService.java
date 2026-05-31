package com.pullcat.service.analysis;

import com.pullcat.model.AnalysisResult;
import com.pullcat.model.Issue;
import com.pullcat.model.ReviewSession;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CompareService {

    private final ReviewRepository reviewRepository;

    public CompareService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public Map<String, Object> compare(String reviewId1, String reviewId2) {
        ReviewSession r1 = reviewRepository.findById(reviewId1);
        ReviewSession r2 = reviewRepository.findById(reviewId2);

        if (r1 == null || r2 == null) {
            return Map.of("error", "One or both reviews not found");
        }

        Set<String> set1 = issueKeys(r1);
        Set<String> set2 = issueKeys(r2);

        Set<String> newIssues = new LinkedHashSet<>(set2);
        newIssues.removeAll(set1);

        Set<String> fixedIssues = new LinkedHashSet<>(set1);
        fixedIssues.removeAll(set2);

        Set<String> persistent = new LinkedHashSet<>(set1);
        persistent.retainAll(set2);

        return Map.of(
                "review1", Map.of("id", reviewId1, "prUrl", r1.getPrUrl()),
                "review2", Map.of("id", reviewId2, "prUrl", r2.getPrUrl()),
                "newCount", newIssues.size(),
                "fixedCount", fixedIssues.size(),
                "persistentCount", persistent.size(),
                "totalIssues1", totalIssueCount(r1),
                "totalIssues2", totalIssueCount(r2)
        );
    }

    private Set<String> issueKeys(ReviewSession session) {
        Set<String> keys = new LinkedHashSet<>();
        for (AnalysisResult result : session.getAnalyses().values()) {
            if (result.getIssues() != null) {
                for (Issue issue : result.getIssues()) {
                    String file = issue.getFile() != null ? issue.getFile() : "";
                    String line = issue.getLine() != null ? String.valueOf(issue.getLine()) : "";
                    String title = issue.getTitle() != null ? issue.getTitle().substring(0, Math.min(issue.getTitle().length(), 50)) : "";
                    keys.add(file + ":" + line + ":" + title);
                }
            }
        }
        return keys;
    }

    private int totalIssueCount(ReviewSession session) {
        int count = 0;
        for (AnalysisResult result : session.getAnalyses().values()) {
            if (result.getIssues() != null) count += result.getIssues().size();
        }
        return count;
    }
}

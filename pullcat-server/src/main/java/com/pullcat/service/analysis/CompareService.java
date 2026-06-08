package com.pullcat.service.analysis;

import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.CompareReviewsRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.dto.resp.ReviewRefRespDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class CompareService {

    private final ReviewRepository reviewRepository;

    public CompareService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public CompareReviewsRespDTO compare(String reviewId1, String reviewId2) {
        ReviewSessionRespDTO r1 = reviewRepository.findById(reviewId1);
        ReviewSessionRespDTO r2 = reviewRepository.findById(reviewId2);

        if (r1 == null || r2 == null) {
            throw new IllegalArgumentException("One or both reviews not found");
        }

        Set<String> set1 = issueKeys(r1);
        Set<String> set2 = issueKeys(r2);

        Set<String> newIssues = new LinkedHashSet<>(set2);
        newIssues.removeAll(set1);

        Set<String> fixedIssues = new LinkedHashSet<>(set1);
        fixedIssues.removeAll(set2);

        Set<String> persistent = new LinkedHashSet<>(set1);
        persistent.retainAll(set2);

        CompareReviewsRespDTO response = new CompareReviewsRespDTO();
        response.setReview1(new ReviewRefRespDTO(reviewId1, r1.getPrUrl()));
        response.setReview2(new ReviewRefRespDTO(reviewId2, r2.getPrUrl()));
        response.setNewCount(newIssues.size());
        response.setFixedCount(fixedIssues.size());
        response.setPersistentCount(persistent.size());
        response.setTotalIssues1(totalIssueCount(r1));
        response.setTotalIssues2(totalIssueCount(r2));
        return response;
    }

    private Set<String> issueKeys(ReviewSessionRespDTO session) {
        Set<String> keys = new LinkedHashSet<>();
        for (AnalysisResultRespDTO result : session.getAnalyses().values()) {
            if (result.getIssues() != null) {
                for (IssueRespDTO issue : result.getIssues()) {
                    String file = issue.getFile() != null ? issue.getFile() : "";
                    String line = issue.getLine() != null ? String.valueOf(issue.getLine()) : "";
                    String title = issue.getTitle() != null ? issue.getTitle().substring(0, Math.min(issue.getTitle().length(), 50)) : "";
                    keys.add(file + ":" + line + ":" + title);
                }
            }
        }
        return keys;
    }

    private int totalIssueCount(ReviewSessionRespDTO session) {
        int count = 0;
        for (AnalysisResultRespDTO result : session.getAnalyses().values()) {
            if (result.getIssues() != null) count += result.getIssues().size();
        }
        return count;
    }
}

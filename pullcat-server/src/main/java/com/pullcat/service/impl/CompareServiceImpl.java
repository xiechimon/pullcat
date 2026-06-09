package com.pullcat.service.impl;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import com.pullcat.dto.resp.CompareReviewsRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.dto.resp.ReviewRefRespDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.service.CompareService;
import com.pullcat.service.analysis.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompareServiceImpl implements CompareService {

    private final ReviewRepository reviewRepository;

    @Override
    public CompareReviewsRespDTO compare(List<String> reviewIds) {
        if (reviewIds == null || reviewIds.size() != 2) {
            throw new ClientException(CommonErrorCodeEnum.CLIENT_ERROR.code(), "必须提供 2 个 reviewIds");
        }
        return compare(reviewIds.get(0), reviewIds.get(1));
    }

    @Override
    public CompareReviewsRespDTO compare(String reviewId1, String reviewId2) {
        ReviewSessionRespDTO r1 = reviewRepository.findById(reviewId1);
        ReviewSessionRespDTO r2 = reviewRepository.findById(reviewId2);

        if (r1 == null || r2 == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
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
                    String title = issue.getTitle() != null
                            ? issue.getTitle().substring(0, Math.min(issue.getTitle().length(), 50))
                            : "";
                    keys.add(file + ":" + line + ":" + title);
                }
            }
        }
        return keys;
    }

    private int totalIssueCount(ReviewSessionRespDTO session) {
        int count = 0;
        for (AnalysisResultRespDTO result : session.getAnalyses().values()) {
            if (result.getIssues() != null) {
                count += result.getIssues().size();
            }
        }
        return count;
    }
}

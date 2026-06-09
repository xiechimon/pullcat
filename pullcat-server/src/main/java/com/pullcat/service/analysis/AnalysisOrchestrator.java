package com.pullcat.service.analysis;

import com.pullcat.dto.req.PublishReqDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;

public interface AnalysisOrchestrator {

    ReviewSessionRespDTO createSession(String prUrl, String userId);

    void startReviewAsync(ReviewSessionRespDTO session);

    ReviewSessionRespDTO publishReview(String reviewId, PublishReqDTO requestParam);
}

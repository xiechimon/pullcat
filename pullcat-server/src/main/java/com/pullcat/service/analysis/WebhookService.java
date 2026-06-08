package com.pullcat.service.analysis;

import com.pullcat.dto.resp.ReviewSessionRespDTO;
import org.springframework.stereotype.Service;

@Service
public class WebhookService {

    private final AnalysisOrchestrator orchestrator;
    private final ReviewRepository reviewRepository;

    public WebhookService(AnalysisOrchestrator orchestrator, ReviewRepository reviewRepository) {
        this.orchestrator = orchestrator;
        this.reviewRepository = reviewRepository;
    }

    public void triggerReview(String prUrl) {
        ReviewSessionRespDTO session = orchestrator.createSession(prUrl, null);
        reviewRepository.save(session);
        orchestrator.startReviewAsync(session);
    }
}

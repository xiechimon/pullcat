package com.pullcat.controller;

import com.pullcat.service.analysis.ReviewRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/repos/{owner}/{repo}/auto-publish")
public class AutoPublishController {

    private final ReviewRepository reviewRepository;

    public AutoPublishController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> get(@PathVariable String owner, @PathVariable String repo) {
        return ResponseEntity.ok(Map.of("enabled", reviewRepository.isAutoPublishEnabled(owner, repo)));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> set(@PathVariable String owner, @PathVariable String repo,
                                                   @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        reviewRepository.setAutoPublishEnabled(owner, repo, enabled);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }
}

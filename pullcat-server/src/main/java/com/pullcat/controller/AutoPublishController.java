package com.pullcat.controller;

import com.pullcat.service.analysis.ReviewRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class AutoPublishController {

    private final ReviewRepository reviewRepository;

    public AutoPublishController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/api/auto-publish")
    public ResponseEntity<List<Map<String, Object>>> list() {
        List<String> repos = reviewRepository.listAutoPublishRepos();
        List<Map<String, Object>> result = repos.stream()
                .map(r -> {
                    String[] parts = r.split("/", 2);
                    return Map.<String, Object>of("owner", parts[0], "repo", parts[1], "enabled", true);
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/repos/{owner}/{repo}/auto-publish")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String owner, @PathVariable String repo) {
        return ResponseEntity.ok(Map.of("enabled", reviewRepository.isAutoPublishEnabled(owner, repo)));
    }

    @PutMapping("/api/repos/{owner}/{repo}/auto-publish")
    public ResponseEntity<Map<String, Object>> set(@PathVariable String owner, @PathVariable String repo,
                                                   @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        reviewRepository.setAutoPublishEnabled(owner, repo, enabled);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    @DeleteMapping("/api/repos/{owner}/{repo}/auto-publish")
    public ResponseEntity<Map<String, Object>> disable(@PathVariable String owner, @PathVariable String repo) {
        reviewRepository.setAutoPublishEnabled(owner, repo, false);
        return ResponseEntity.ok(Map.of("enabled", false));
    }
}

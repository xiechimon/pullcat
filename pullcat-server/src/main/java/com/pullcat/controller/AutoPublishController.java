package com.pullcat.controller;

import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dto.req.AutoPublishToggleReqDTO;
import com.pullcat.dto.resp.AutoPublishRepoRespDTO;
import com.pullcat.dto.resp.BooleanStatusRespDTO;
import com.pullcat.service.analysis.ReviewRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AutoPublishController {

    private final ReviewRepository reviewRepository;

    public AutoPublishController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/api/auto-publish")
    public Result<List<AutoPublishRepoRespDTO>> list() {
        List<String> repos = reviewRepository.listAutoPublishRepos();
        List<AutoPublishRepoRespDTO> result = repos.stream()
                .map(r -> {
                    String[] parts = r.split("/", 2);
                    return new AutoPublishRepoRespDTO(parts[0], parts[1], true);
                })
                .toList();
        return Results.success(result);
    }

    @GetMapping("/api/repos/{owner}/{repo}/auto-publish")
    public Result<BooleanStatusRespDTO> get(@PathVariable String owner, @PathVariable String repo) {
        return Results.success(new BooleanStatusRespDTO(reviewRepository.isAutoPublishEnabled(owner, repo)));
    }

    @PutMapping("/api/repos/{owner}/{repo}/auto-publish")
    public Result<BooleanStatusRespDTO> set(@PathVariable String owner, @PathVariable String repo,
                                            @RequestBody AutoPublishToggleReqDTO requestParam) {
        boolean enabled = Boolean.TRUE.equals(requestParam.getEnabled());
        reviewRepository.setAutoPublishEnabled(owner, repo, enabled);
        return Results.success(new BooleanStatusRespDTO(enabled));
    }

    @DeleteMapping("/api/repos/{owner}/{repo}/auto-publish")
    public Result<BooleanStatusRespDTO> disable(@PathVariable String owner, @PathVariable String repo) {
        reviewRepository.setAutoPublishEnabled(owner, repo, false);
        return Results.success(new BooleanStatusRespDTO(false));
    }
}

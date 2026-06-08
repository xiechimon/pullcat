package com.pullcat.controller;

import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dto.resp.RepoStatsRespDTO;
import com.pullcat.dto.resp.StatsOverviewRespDTO;
import com.pullcat.service.analysis.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<Result<StatsOverviewRespDTO>> getOverview() {
        return ResponseEntity.ok(Results.success(statsService.getOverview()));
    }

    @GetMapping("/repos/{owner}/{repo}/stats")
    public ResponseEntity<Result<RepoStatsRespDTO>> getRepoStats(
            @PathVariable String owner, @PathVariable String repo) {
        return ResponseEntity.ok(Results.success(statsService.getRepoStats(owner, repo)));
    }
}

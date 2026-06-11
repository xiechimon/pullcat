package com.pullcat.controller;

import com.pullcat.common.biz.user.CurrentLogin;
import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dto.resp.RepoStatsRespDTO;
import com.pullcat.dto.resp.StatsOverviewRespDTO;
import com.pullcat.service.StatsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pullcat/v1")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats/overview")
    public Result<StatsOverviewRespDTO> getOverview(@CurrentLogin String login) {
        return Results.success(statsService.getOverview(login));
    }

    @GetMapping("/repos/{owner}/{repo}/stats")
    public Result<RepoStatsRespDTO> getRepoStats(
            @PathVariable String owner, @PathVariable String repo) {
        return Results.success(statsService.getRepoStats(owner, repo));
    }
}

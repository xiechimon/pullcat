package com.pullcat.controller;

import com.pullcat.service.analysis.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return ResponseEntity.ok(statsService.getOverview());
    }

    @GetMapping("/repos/{owner}/{repo}/stats")
    public ResponseEntity<Map<String, Object>> getRepoStats(
            @PathVariable String owner, @PathVariable String repo) {
        return ResponseEntity.ok(statsService.getRepoStats(owner, repo));
    }
}

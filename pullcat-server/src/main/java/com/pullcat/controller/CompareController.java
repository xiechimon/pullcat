package com.pullcat.controller;

import com.pullcat.service.analysis.CompareService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CompareController {

    private final CompareService compareService;

    public CompareController(CompareService compareService) {
        this.compareService = compareService;
    }

    @PostMapping("/reviews/compare")
    public ResponseEntity<Map<String, Object>> compare(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        var ids = (java.util.List<String>) body.get("reviewIds");
        if (ids == null || ids.size() != 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Provide exactly 2 reviewIds"));
        }
        return ResponseEntity.ok(compareService.compare(ids.get(0), ids.get(1)));
    }
}

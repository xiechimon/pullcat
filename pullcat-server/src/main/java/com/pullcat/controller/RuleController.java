package com.pullcat.controller;

import com.pullcat.model.Issue;
import com.pullcat.model.Rule;
import com.pullcat.service.analysis.RuleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/repos/{owner}/{repo}/rules")
public class RuleController {

    private final RuleRepository ruleRepository;

    public RuleController(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @GetMapping
    public ResponseEntity<List<Rule>> list(@PathVariable String owner, @PathVariable String repo) {
        return ResponseEntity.ok(ruleRepository.findByRepo(owner, repo));
    }

    @PostMapping
    public ResponseEntity<Rule> create(@PathVariable String owner, @PathVariable String repo, @RequestBody Rule rule) {
        rule.setId(UUID.randomUUID().toString());
        rule.setRepoOwner(owner);
        rule.setRepoName(repo);
        ruleRepository.save(rule);
        return ResponseEntity.ok(rule);
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<Rule> update(@PathVariable String owner, @PathVariable String repo,
                                       @PathVariable String ruleId, @RequestBody Rule rule) {
        rule.setId(ruleId);
        rule.setRepoOwner(owner);
        rule.setRepoName(repo);
        ruleRepository.save(rule);
        return ResponseEntity.ok(rule);
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String owner, @PathVariable String repo,
                                                       @PathVariable String ruleId) {
        ruleRepository.delete(owner, repo, ruleId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @PutMapping("/{ruleId}/toggle")
    public ResponseEntity<Rule> toggle(@PathVariable String owner, @PathVariable String repo,
                                        @PathVariable String ruleId) {
        var opt = ruleRepository.findById(owner, repo, ruleId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Rule rule = opt.get();
        rule.setEnabled(!rule.isEnabled());
        ruleRepository.save(rule);
        return ResponseEntity.ok(rule);
    }
}

package com.pullcat.controller;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.dao.entity.RuleDO;
import com.pullcat.dto.req.RuleUpsertReqDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.RuleRespDTO;
import com.pullcat.service.analysis.RuleRepository;
import com.pullcat.service.analysis.RuleSuggestionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/repos/{owner}/{repo}/rules")
public class RuleController {

    private final RuleRepository ruleRepository;
    private final RuleSuggestionService ruleSuggestionService;

    public RuleController(RuleRepository ruleRepository, RuleSuggestionService ruleSuggestionService) {
        this.ruleRepository = ruleRepository;
        this.ruleSuggestionService = ruleSuggestionService;
    }

    @GetMapping
    public Result<List<RuleRespDTO>> list(@PathVariable String owner, @PathVariable String repo) {
        return Results.success(ruleRepository.findByRepo(owner, repo).stream().map(this::toRuleRespDTO).toList());
    }

    @GetMapping("/suggestions")
    public Result<List<RuleRespDTO>> getSuggestions(@PathVariable String owner, @PathVariable String repo) {
        return Results.success(ruleSuggestionService.getSuggestions(owner, repo).stream().map(this::toRuleRespDTO).toList());
    }

    @PostMapping
    public Result<RuleRespDTO> create(@PathVariable String owner, @PathVariable String repo,
                                      @RequestBody RuleUpsertReqDTO requestParam) {
        RuleDO rule = toRuleDO(requestParam);
        rule.setId(UUID.randomUUID().toString());
        rule.setRepoOwner(owner);
        rule.setRepoName(repo);
        ruleRepository.save(rule);
        return Results.success(toRuleRespDTO(rule));
    }

    @PutMapping("/{ruleId}")
    public Result<RuleRespDTO> update(@PathVariable String owner, @PathVariable String repo,
                                      @PathVariable String ruleId, @RequestBody RuleUpsertReqDTO requestParam) {
        RuleDO rule = toRuleDO(requestParam);
        rule.setId(ruleId);
        rule.setRepoOwner(owner);
        rule.setRepoName(repo);
        ruleRepository.save(rule);
        return Results.success(toRuleRespDTO(rule));
    }

    @DeleteMapping("/{ruleId}")
    public Result<DeletedRespDTO> delete(@PathVariable String owner, @PathVariable String repo,
                                         @PathVariable String ruleId) {
        ruleRepository.delete(owner, repo, ruleId);
        return Results.success(new DeletedRespDTO(true));
    }

    @PutMapping("/{ruleId}/toggle")
    public Result<RuleRespDTO> toggle(@PathVariable String owner, @PathVariable String repo,
                                      @PathVariable String ruleId) {
        var opt = ruleRepository.findById(owner, repo, ruleId);
        if (opt.isEmpty()) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "规则不存在");
        }
        RuleDO rule = opt.get();
        rule.setEnabled(!rule.isEnabled());
        ruleRepository.save(rule);
        return Results.success(toRuleRespDTO(rule));
    }

    private RuleDO toRuleDO(RuleUpsertReqDTO requestParam) {
        RuleDO rule = new RuleDO();
        rule.setType(requestParam.getType());
        rule.setPattern(requestParam.getPattern());
        rule.setSeverity(requestParam.getSeverity());
        rule.setName(requestParam.getName());
        rule.setMessage(requestParam.getMessage());
        rule.setSuggestion(requestParam.getSuggestion());
        rule.setEnabled(requestParam.isEnabled());
        return rule;
    }

    private RuleRespDTO toRuleRespDTO(RuleDO rule) {
        RuleRespDTO response = new RuleRespDTO();
        response.setId(rule.getId());
        response.setName(rule.getName());
        response.setType(rule.getType());
        response.setPattern(rule.getPattern());
        response.setSeverity(rule.getSeverity());
        response.setMessage(rule.getMessage());
        response.setSuggestion(rule.getSuggestion());
        response.setEnabled(rule.isEnabled());
        return response;
    }
}

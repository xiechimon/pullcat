package com.pullcat.controller;

import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dto.req.RuleUpsertReqDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.RuleRespDTO;
import com.pullcat.service.analysis.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规则控制层
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/repos/{owner}/{repo}/rules")
public class RuleController {

    private final RuleService ruleService;

    /**
     * 查询规则列表
     */
    @GetMapping
    public Result<List<RuleRespDTO>> list(@PathVariable String owner, @PathVariable String repo) {
        return Results.success(ruleService.list(owner, repo));
    }

    /**
     * 查询规则建议
     */
    @GetMapping("/suggestions")
    public Result<List<RuleRespDTO>> getSuggestions(@PathVariable String owner, @PathVariable String repo) {
        return Results.success(ruleService.getSuggestions(owner, repo));
    }

    /**
     * 创建规则
     */
    @PostMapping
    public Result<RuleRespDTO> create(@PathVariable String owner, @PathVariable String repo,
                                      @RequestBody RuleUpsertReqDTO requestParam) {
        return Results.success(ruleService.create(owner, repo, requestParam));
    }

    /**
     * 更新规则
     */
    @PutMapping("/{ruleId}")
    public Result<RuleRespDTO> update(@PathVariable String owner, @PathVariable String repo,
                                      @PathVariable String ruleId, @RequestBody RuleUpsertReqDTO requestParam) {
        return Results.success(ruleService.update(owner, repo, ruleId, requestParam));
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{ruleId}")
    public Result<DeletedRespDTO> delete(@PathVariable String owner, @PathVariable String repo,
                                         @PathVariable String ruleId) {
        return Results.success(ruleService.delete(owner, repo, ruleId));
    }

    /**
     * 切换规则启用状态
     */
    @PutMapping("/{ruleId}/toggle")
    public Result<RuleRespDTO> toggle(@PathVariable String owner, @PathVariable String repo,
                                      @PathVariable String ruleId) {
        return Results.success(ruleService.toggle(owner, repo, ruleId));
    }
}

package com.pullcat.controller;

import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dto.req.AutoPublishToggleReqDTO;
import com.pullcat.dto.resp.AutoPublishRepoRespDTO;
import com.pullcat.dto.resp.BooleanStatusRespDTO;
import com.pullcat.service.analysis.AutoPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自动发布控制层
 */
@RestController
@RequiredArgsConstructor
public class AutoPublishController {

    private final AutoPublishService autoPublishService;

    /**
     * 查询所有已启用自动发布的仓库
     */
    @GetMapping("/api/auto-publish")
    public Result<List<AutoPublishRepoRespDTO>> list() {
        return Results.success(autoPublishService.listAutoPublishRepos());
    }

    /**
     * 查询指定仓库的自动发布状态
     */
    @GetMapping("/api/repos/{owner}/{repo}/auto-publish")
    public Result<BooleanStatusRespDTO> get(@PathVariable String owner, @PathVariable String repo) {
        return Results.success(autoPublishService.getStatus(owner, repo));
    }

    /**
     * 设置指定仓库的自动发布状态
     */
    @PutMapping("/api/repos/{owner}/{repo}/auto-publish")
    public Result<BooleanStatusRespDTO> set(@PathVariable String owner, @PathVariable String repo,
                                            @RequestBody AutoPublishToggleReqDTO requestParam) {
        return Results.success(autoPublishService.setEnabled(owner, repo, requestParam.getEnabled()));
    }

    /**
     * 禁用指定仓库的自动发布
     */
    @DeleteMapping("/api/repos/{owner}/{repo}/auto-publish")
    public Result<BooleanStatusRespDTO> disable(@PathVariable String owner, @PathVariable String repo) {
        return Results.success(autoPublishService.setEnabled(owner, repo, false));
    }
}

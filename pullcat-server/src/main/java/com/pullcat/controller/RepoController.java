package com.pullcat.controller;

import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dto.req.CreateRepoReqDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.RepoRespDTO;
import com.pullcat.service.RepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仓库控制层
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pullcat/v1/repos")
public class RepoController {

    private final RepoService repoService;

    /**
     * 查询所有仓库
     */
    @GetMapping
    public Result<List<RepoRespDTO>> listRepos() {
        return Results.success(repoService.listRepos());
    }

    /**
     * 新增仓库
     */
    @PostMapping
    public Result<RepoRespDTO> addRepo(@RequestBody CreateRepoReqDTO requestParam) {
        return Results.success(repoService.addRepo(requestParam));
    }

    /**
     * 删除仓库
     */
    @DeleteMapping("/{owner}/{repo}")
    public Result<DeletedRespDTO> removeRepo(@PathVariable String owner, @PathVariable String repo) {
        return Results.success(repoService.removeRepo(owner, repo));
    }

    /**
     * 查询单个仓库
     */
    @GetMapping("/{owner}/{repo}")
    public Result<RepoRespDTO> getRepo(@PathVariable String owner, @PathVariable String repo) {
        return Results.success(repoService.getRepo(owner, repo));
    }
}

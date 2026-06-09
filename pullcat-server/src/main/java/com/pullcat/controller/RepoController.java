package com.pullcat.controller;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.dao.entity.RepoDO;
import com.pullcat.dto.req.CreateRepoReqDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.RepoRespDTO;
import com.pullcat.service.analysis.RepoRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repos")
public class RepoController {

    private final RepoRepository repoRepository;

    public RepoController(RepoRepository repoRepository) {
        this.repoRepository = repoRepository;
    }

    @GetMapping
    public Result<List<RepoRespDTO>> listRepos() {
        return Results.success(repoRepository.findAll().stream().map(this::toRepoRespDTO).toList());
    }

    @PostMapping
    public Result<RepoRespDTO> addRepo(@RequestBody CreateRepoReqDTO requestParam) {
        String owner = requestParam.getOwner();
        String repo = requestParam.getRepo();
        if (owner == null || repo == null) {
            throw new ClientException(CommonErrorCodeEnum.CLIENT_ERROR.code(), "owner 和 repo 不能为空");
        }

        RepoDO r = new RepoDO(owner, repo);
        if (requestParam.getDescription() != null) {
            r.setDescription(requestParam.getDescription());
        }
        repoRepository.save(r);
        return Results.success(toRepoRespDTO(r));
    }

    @DeleteMapping("/{owner}/{repo}")
    public Result<DeletedRespDTO> removeRepo(@PathVariable String owner, @PathVariable String repo) {
        if (!repoRepository.exists(owner, repo)) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "仓库不存在");
        }
        repoRepository.delete(owner, repo);
        return Results.success(new DeletedRespDTO(true));
    }

    @GetMapping("/{owner}/{repo}")
    public Result<RepoRespDTO> getRepo(@PathVariable String owner, @PathVariable String repo) {
        RepoDO r = repoRepository.findById(owner + "/" + repo);
        if (r == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "仓库不存在");
        }
        return Results.success(toRepoRespDTO(r));
    }

    private RepoRespDTO toRepoRespDTO(RepoDO repoDO) {
        RepoRespDTO response = new RepoRespDTO();
        response.setOwner(repoDO.getOwner());
        response.setRepo(repoDO.getRepo());
        response.setFullName(repoDO.getFullName());
        response.setDescription(repoDO.getDescription());
        response.setStars(repoDO.getStars());
        response.setLanguage(repoDO.getLanguage());
        response.setAddedAt(repoDO.getAddedAt() == null ? null : repoDO.getAddedAt().toString());
        return response;
    }
}

package com.pullcat.service;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.dao.entity.RepoDO;
import com.pullcat.dto.req.CreateRepoReqDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.RepoRespDTO;
import com.pullcat.service.analysis.RepoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 仓库业务服务
 */
@Service
@RequiredArgsConstructor
public class RepoService {

    private final RepoRepository repoRepository;

    /**
     * 查询所有仓库
     */
    public List<RepoRespDTO> listRepos() {
        return repoRepository.findAll().stream().map(this::toRespDTO).toList();
    }

    /**
     * 新增仓库
     */
    public RepoRespDTO addRepo(CreateRepoReqDTO req) {
        String owner = req.getOwner();
        String repo = req.getRepo();
        if (owner == null || repo == null) {
            throw new ClientException(CommonErrorCodeEnum.CLIENT_ERROR.code(), "owner 和 repo 不能为空");
        }

        RepoDO repoDO = new RepoDO(owner, repo);
        if (req.getDescription() != null) {
            repoDO.setDescription(req.getDescription());
        }
        repoRepository.save(repoDO);
        return toRespDTO(repoDO);
    }

    /**
     * 删除仓库
     */
    public DeletedRespDTO removeRepo(String owner, String repo) {
        if (!repoRepository.exists(owner, repo)) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "仓库不存在");
        }

        repoRepository.delete(owner, repo);
        return new DeletedRespDTO(true);
    }

    /**
     * 查询单个仓库
     */
    public RepoRespDTO getRepo(String owner, String repo) {
        RepoDO repoDO = repoRepository.findById(owner + "/" + repo);
        if (repoDO == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "仓库不存在");
        }

        return toRespDTO(repoDO);
    }

    private RepoRespDTO toRespDTO(RepoDO repoDO) {
        RepoRespDTO resp = new RepoRespDTO();
        resp.setOwner(repoDO.getOwner());
        resp.setRepo(repoDO.getRepo());
        resp.setFullName(repoDO.getFullName());
        resp.setDescription(repoDO.getDescription());
        resp.setStars(repoDO.getStars());
        resp.setLanguage(repoDO.getLanguage());
        resp.setAddedAt(repoDO.getAddedAt() == null ? null : repoDO.getAddedAt().toString());
        return resp;
    }
}

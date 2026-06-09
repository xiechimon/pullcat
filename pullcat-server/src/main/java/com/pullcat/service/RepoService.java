package com.pullcat.service;

import com.pullcat.dto.req.CreateRepoReqDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.RepoRespDTO;

import java.util.List;

/**
 * 仓库业务服务
 */
public interface RepoService {

    /**
     * 查询所有仓库
     */
    List<RepoRespDTO> listRepos();

    /**
     * 新增仓库
     */
    RepoRespDTO addRepo(CreateRepoReqDTO req);

    /**
     * 删除仓库
     */
    DeletedRespDTO removeRepo(String owner, String repo);

    /**
     * 查询单个仓库
     */
    RepoRespDTO getRepo(String owner, String repo);
}

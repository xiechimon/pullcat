package com.pullcat.service;

import com.pullcat.dto.resp.AutoPublishRepoRespDTO;
import com.pullcat.dto.resp.BooleanStatusRespDTO;

import java.util.List;

/**
 * 自动发布业务服务
 */
public interface AutoPublishService {

    /**
     * 查询所有已启用自动发布的仓库列表
     */
    List<AutoPublishRepoRespDTO> listAutoPublishRepos();

    /**
     * 查询指定仓库的自动发布状态
     */
    BooleanStatusRespDTO getStatus(String owner, String repo);

    /**
     * 设置指定仓库的自动发布状态
     */
    BooleanStatusRespDTO setEnabled(String owner, String repo, Boolean enabled);
}

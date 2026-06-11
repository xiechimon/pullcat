package com.pullcat.service;

import com.pullcat.dto.resp.RepoStatsRespDTO;
import com.pullcat.dto.resp.StatsOverviewRespDTO;

/**
 * 统计业务服务
 */
public interface StatsService {

    /**
     * 查询总体统计
     */
    StatsOverviewRespDTO getOverview(String login);

    /**
     * 查询仓库统计
     */
    RepoStatsRespDTO getRepoStats(String owner, String repo);
}

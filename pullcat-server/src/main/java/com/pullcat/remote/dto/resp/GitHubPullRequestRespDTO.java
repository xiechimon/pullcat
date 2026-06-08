package com.pullcat.remote.dto.resp;

import lombok.Data;

/**
 * GitHub Pull Request 响应 DTO。
 */
@Data
public class GitHubPullRequestRespDTO {

    /**
     * 标题
     */
    private String title;

    /**
     * 描述正文
     */
    private String body;

    /**
     * 基线分支信息
     */
    private GitHubBranchRefRespDTO base;

    /**
     * 目标分支信息
     */
    private GitHubBranchRefRespDTO head;

    /**
     * 变更文件数
     */
    private int changedFiles;

    /**
     * 新增行数
     */
    private int additions;

    /**
     * 删除行数
     */
    private int deletions;
}

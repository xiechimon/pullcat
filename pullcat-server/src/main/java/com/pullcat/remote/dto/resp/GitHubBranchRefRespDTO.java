package com.pullcat.remote.dto.resp;

import lombok.Data;

/**
 * GitHub 分支引用响应 DTO。
 */
@Data
public class GitHubBranchRefRespDTO {

    /**
     * 分支名
     */
    private String ref;

    /**
     * 提交 SHA
     */
    private String sha;
}

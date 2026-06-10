package com.pullcat.remote.dto.resp;

import lombok.Data;

/**
 * GitHub 评论响应 DTO。
 */
@Data
public class GitHubCommentRespDTO {

    /**
     * 评论 ID
     */
    private Long id;

    /**
     * 评论用户
     */
    private GitHubUserRespDTO user;

    /**
     * 评论正文
     */
    private String body;
}

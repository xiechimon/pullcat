package com.pullcat.remote.dto.req;

import lombok.Data;

import java.util.List;

/**
 * GitHub review 请求 DTO。
 */
@Data
public class GitHubReviewReqDTO {

    /**
     * review 事件类型
     */
    private String event;

    /**
     * review 摘要正文
     */
    private String body;

    /**
     * 行级评论列表
     */
    private List<GitHubReviewCommentReqDTO> comments;
}

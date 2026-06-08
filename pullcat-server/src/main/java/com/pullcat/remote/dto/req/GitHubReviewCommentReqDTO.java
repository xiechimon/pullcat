package com.pullcat.remote.dto.req;

import lombok.Data;

/**
 * GitHub review 行级评论请求 DTO。
 */
@Data
public class GitHubReviewCommentReqDTO {

    /**
     * 文件路径
     */
    private String path;

    /**
     * 行号
     */
    private Integer line;

    /**
     * 代码侧边
     */
    private String side;

    /**
     * 评论正文
     */
    private String body;
}

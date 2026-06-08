package com.pullcat.dto.resp;

import lombok.Data;

/**
 * 发布审查响应 DTO
 */
@Data
public class PublishReviewRespDTO {

    /**
     * 发布后状态
     */
    private String status;

    /**
     * 评论 ID
     */
    private Long commentId;

    /**
     * Pull Request 链接
     */
    private String prUrl;
}

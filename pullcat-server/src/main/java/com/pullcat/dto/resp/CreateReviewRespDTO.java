package com.pullcat.dto.resp;

import lombok.Data;

/**
 * 创建审查响应 DTO
 */
@Data
public class CreateReviewRespDTO {

    /**
     * 审查 ID
     */
    private String reviewId;

    /**
     * 当前状态
     */
    private String status;

    /**
     * SSE 链接
     */
    private String sseUrl;
}

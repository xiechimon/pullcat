package com.pullcat.dto.req;

import lombok.Data;

/**
 * 创建审查请求 DTO
 */
@Data
public class CreateReviewReqDTO {

    /**
     * Pull Request 链接
     */
    private String prUrl;
}

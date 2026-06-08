package com.pullcat.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审查引用响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRefRespDTO {

    /**
     * 审查 ID
     */
    private String id;

    /**
     * Pull Request 链接
     */
    private String prUrl;
}

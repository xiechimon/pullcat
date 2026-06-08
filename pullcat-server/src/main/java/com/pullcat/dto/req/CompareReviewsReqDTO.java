package com.pullcat.dto.req;

import lombok.Data;

import java.util.List;

/**
 * 审查结果对比请求 DTO
 */
@Data
public class CompareReviewsReqDTO {

    /**
     * 参与对比的审查 ID 列表
     */
    private List<String> reviewIds;
}

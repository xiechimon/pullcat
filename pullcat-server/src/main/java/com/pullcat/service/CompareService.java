package com.pullcat.service;

import com.pullcat.dto.resp.CompareReviewsRespDTO;

import java.util.List;

/**
 * 审查对比业务服务
 */
public interface CompareService {

    /**
     * 承接控制层的组合参数校验
     */
    CompareReviewsRespDTO compare(List<String> reviewIds);

    /**
     * 对比两次审查结果
     */
    CompareReviewsRespDTO compare(String reviewId1, String reviewId2);
}

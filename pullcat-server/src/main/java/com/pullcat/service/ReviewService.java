package com.pullcat.service;

import com.pullcat.dto.req.PublishReqDTO;
import com.pullcat.dto.resp.CreateReviewRespDTO;
import com.pullcat.dto.resp.PublishReviewRespDTO;
import com.pullcat.dto.resp.ReviewListRespDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 审查业务服务
 */
public interface ReviewService {

    /**
     * 分页查询审查列表
     */
    ReviewListRespDTO listReviews(int page, int size, String repo, String login);

    /**
     * 获取单条审查详情
     */
    ReviewSessionRespDTO getReview(String id, String login);

    /**
     * 删除审查记录
     */
    void deleteReview(String id, String login);

    /**
     * 创建审查会话并返回初始信息
     *
     * @param prUrl PR 链接
     * @param login 当前登录用户
     * @return 审查会话信息响应实体，包括会话 ID 和 SSE 地址
     */
    CreateReviewRespDTO createReview(String prUrl, String login);

    /**
     * 将审查结果发布到 PR 评论
     */
    PublishReviewRespDTO publishReview(String id, PublishReqDTO requestParam, String login);

    /**
     * SSE 流式推送分析进度与结果
     */
    SseEmitter startSseStream(String id, String login);

    /**
     * Webhook 触发审查
     */
    void triggerReview(String prUrl, Long installationId);
}

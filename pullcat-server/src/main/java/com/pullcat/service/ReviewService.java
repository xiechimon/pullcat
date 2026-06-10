package com.pullcat.service;

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
     * 创建审查会话并返回初始信息
     *
     * @param prUrl PR 链接
     * @param login 当前登录用户
     * @return 审查会话信息响应实体，包括会话 ID 和 SSE 地址
     */
    CreateReviewRespDTO createReview(String prUrl, String login);

    /**
     * 分页查询审查列表
     *
     * @param page 页码，从 0 开始
     * @param size 页大小
     * @param repo 可选的仓库名称过滤参数，格式为 "owner/repo"
     * @param login 当前登录用户
     * @return 审查列表响应实体
     */
    ReviewListRespDTO listReviews(int page, int size, String repo, String login);

    /**
     * 获取单条审查详情
     *
     * @param id 审查id
     * @param login 当前登录用户
     * @return 审查会话详情响应实体
     */
    ReviewSessionRespDTO getReview(String id, String login);

    /**
     * 删除审查记录
     *
     * @param id 审查id
     * @param login 当前登录用户
     */
    void deleteReview(String id, String login);

    /**
     * SSE 流式推送分析进度与结果
     *
     * @param id 审查id
     * @param login 当前登录用户
     * @return SseEmitter 实例，用于向客户端推送审查进度和结果
     */
    SseEmitter startSseStream(String id, String login);


    /**
     * 将审查结果发布到 PR 评论
     *
     * @param id 审查 id
     * @param login 当前登录用户
     * @return 发布审查响应实体
     */
    PublishReviewRespDTO publishReview(String id, String login);


    /**
     * Webhook 触发审查，立即 post pending commit status
     *
     * @param prUrl          PR 链接
     * @param installationId GitHub App 安装 ID，webhook 触发时提供
     * @param headSha        PR head commit SHA，用于 commit status 回写，无则传 null
     */
    void triggerReview(String prUrl, Long installationId, String headSha);
}

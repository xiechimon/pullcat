package com.pullcat.service.analysis;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.dto.req.PublishReqDTO;
import com.pullcat.dto.resp.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 审查业务服务
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final AnalysisOrchestrator orchestrator;
    private final ReviewRepository reviewRepository;

    /**
     * 分页查询审查列表
     */
    public ReviewListRespDTO listReviews(int page, int size, String repo, String login) {
        List<ReviewSessionRespDTO> reviews;
        long total;

        if (repo != null && !repo.isBlank()) {
            reviews = reviewRepository.findByRepo(repo, page, size);
            total = reviewRepository.countByRepo(repo);
        } else if (login != null) {
            reviews = reviewRepository.findByLogin(login, page, size);
            total = reviewRepository.countByLogin(login);
        } else {
            reviews = reviewRepository.findAnonymous(page, size);
            total = reviewRepository.countAnonymous();
        }

        ReviewListRespDTO response = new ReviewListRespDTO();
        response.setItems(reviews);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    /**
     * 获取单条审查详情
     */
    public ReviewSessionRespDTO getReview(String id, String login) {
        ReviewSessionRespDTO session = reviewRepository.findById(id);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权访问此审查");
        }
        return session;
    }

    /**
     * 删除审查记录
     */
    public void deleteReview(String id, String login) {
        ReviewSessionRespDTO session = reviewRepository.findById(id);
        if (session == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "审查记录不存在");
        }
        if (!isOwner(session, login)) {
            throw new ClientException(CommonErrorCodeEnum.FORBIDDEN.code(), "无权删除此审查");
        }
        reviewRepository.delete(id);
    }

    /**
     * 创建审查（stub，待任务 3 实现）
     */
    public CreateReviewRespDTO createReview(String prUrl, String login) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * 提交问题反馈（stub，待任务 3 实现）
     */
    public StatusRespDTO submitFeedback(String reviewId, String issueId, boolean accepted, String reason, String login) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * 发布审查结果（stub，待任务 4 实现）
     */
    public PublishReviewRespDTO publishReview(String id, PublishReqDTO requestParam, String login) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * 启动 SSE 流（stub，待任务 4 实现）
     */
    public SseEmitter startSseStream(String id, String login) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * 校验当前用户是否为审查所有者
     */
    private boolean isOwner(ReviewSessionRespDTO session, String login) {
        if (session.getUserId() == null) {
            return login == null;
        }
        return session.getUserId().equals(login);
    }
}

package com.pullcat.service.analysis;

import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.remote.GitHubApiService;

import java.util.List;

/**
 * 审查结果发布到 GitHub PR
 */
public interface ReviewPublisher {

    /**
     * 发布审查结果到 PR
     */
    ReviewSessionRespDTO publishReview(String reviewId);

    /**
     * 尝试自动发布，返回是否成功触发
     */
    boolean tryAutoPublish(ReviewSessionRespDTO session);

    /**
     * 构建仓库约定内容
     */
    String buildConventionContent(GitHubApiService.PRUrl prUrl, List<String> candidates);
}

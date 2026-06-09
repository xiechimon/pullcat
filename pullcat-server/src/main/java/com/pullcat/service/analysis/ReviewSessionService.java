package com.pullcat.service.analysis;

import com.pullcat.dto.resp.ReviewSessionRespDTO;

import java.util.List;

/**
 * 审查会话存储服务
 */
public interface ReviewSessionService {

    void save(ReviewSessionRespDTO session);

    ReviewSessionRespDTO findById(String id);

    List<ReviewSessionRespDTO> findAll(int page, int size);

    List<ReviewSessionRespDTO> findByRepo(String fullName, int page, int size);

    List<ReviewSessionRespDTO> findByLogin(String login, int page, int size);

    long countByLogin(String login);

    List<ReviewSessionRespDTO> findAnonymous(int page, int size);

    long countAnonymous();

    long count();

    long countByRepo(String fullName);

    List<ReviewSessionRespDTO> findAllReviews();

    void delete(String id);

    boolean exists(String id);

    boolean isAutoPublishEnabled(String owner, String repo);

    void setAutoPublishEnabled(String owner, String repo, boolean enabled);

    List<String> listAutoPublishRepos();
}

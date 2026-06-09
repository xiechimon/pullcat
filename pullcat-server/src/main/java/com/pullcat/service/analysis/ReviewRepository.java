package com.pullcat.service.analysis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullcat.common.convention.exception.ServiceException;
import com.pullcat.dao.entity.RepoAutoPublishDO;
import com.pullcat.dao.entity.ReviewDO;
import com.pullcat.dao.mapper.RepoAutoPublishMapper;
import com.pullcat.dao.mapper.ReviewMapper;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class ReviewRepository {

    private final ReviewMapper reviewMapper;

    private final RepoAutoPublishMapper repoAutoPublishMapper;

    private final ObjectMapper objectMapper;

    public ReviewRepository(ReviewMapper reviewMapper,
                            RepoAutoPublishMapper repoAutoPublishMapper,
                            ObjectMapper objectMapper) {
        this.reviewMapper = reviewMapper;
        this.repoAutoPublishMapper = repoAutoPublishMapper;
        this.objectMapper = objectMapper;
    }

    public void save(ReviewSessionRespDTO session) {
        ReviewDO reviewDO = toDO(session);
        if (reviewMapper.selectById(session.getId()) == null) {
            reviewMapper.insert(reviewDO);
        } else {
            reviewMapper.updateById(reviewDO);
        }
    }

    public ReviewSessionRespDTO findById(String id) {
        return toDTO(reviewMapper.selectById(id));
    }

    public List<ReviewSessionRespDTO> findAll(int page, int size) {
        return reviewMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .orderByDesc(ReviewDO::getCreatedAt)
                        .last(limitClause(page, size)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ReviewSessionRespDTO> findByRepo(String fullName, int page, int size) {
        return reviewMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .eq(ReviewDO::getRepositoryFullName, fullName)
                        .orderByDesc(ReviewDO::getCreatedAt)
                        .last(limitClause(page, size)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ReviewSessionRespDTO> findByLogin(String login, int page, int size) {
        return reviewMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .eq(ReviewDO::getUserId, login)
                        .orderByDesc(ReviewDO::getCreatedAt)
                        .last(limitClause(page, size)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public long countByLogin(String login) {
        return reviewMapper.selectCount(Wrappers.<ReviewDO>lambdaQuery()
                .eq(ReviewDO::getUserId, login));
    }

    public List<ReviewSessionRespDTO> findAnonymous(int page, int size) {
        return reviewMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .isNull(ReviewDO::getUserId)
                        .orderByDesc(ReviewDO::getCreatedAt)
                        .last(limitClause(page, size)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public long countAnonymous() {
        return reviewMapper.selectCount(Wrappers.<ReviewDO>lambdaQuery()
                .isNull(ReviewDO::getUserId));
    }

    public long count() {
        return reviewMapper.selectCount(null);
    }

    public long countByRepo(String fullName) {
        return reviewMapper.selectCount(Wrappers.<ReviewDO>lambdaQuery()
                .eq(ReviewDO::getRepositoryFullName, fullName));
    }

    public List<ReviewSessionRespDTO> findAllReviews() {
        return reviewMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .orderByDesc(ReviewDO::getCreatedAt))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public void delete(String id) {
        reviewMapper.deleteById(id);
    }

    public boolean exists(String id) {
        return reviewMapper.selectById(id) != null;
    }

    public boolean isAutoPublishEnabled(String owner, String repo) {
        RepoAutoPublishDO config = repoAutoPublishMapper.selectById(owner + "/" + repo);
        return config != null && config.isEnabled();
    }

    public void setAutoPublishEnabled(String owner, String repo, boolean enabled) {
        String fullName = owner + "/" + repo;
        if (enabled) {
            RepoAutoPublishDO config = new RepoAutoPublishDO(owner, repo, true);
            RepoAutoPublishDO existing = repoAutoPublishMapper.selectById(fullName);
            if (existing != null) {
                config.setCreatedAt(existing.getCreatedAt());
            }
            config.setUpdatedAt(Instant.now());
            if (existing == null) {
                repoAutoPublishMapper.insert(config);
            } else {
                repoAutoPublishMapper.updateById(config);
            }
        } else {
            repoAutoPublishMapper.deleteById(fullName);
        }
    }

    public List<String> listAutoPublishRepos() {
        return repoAutoPublishMapper.selectList(new LambdaQueryWrapper<RepoAutoPublishDO>()
                        .eq(RepoAutoPublishDO::isEnabled, true)
                        .orderByAsc(RepoAutoPublishDO::getFullName))
                .stream()
                .map(RepoAutoPublishDO::getFullName)
                .toList();
    }

    private ReviewDO toDO(ReviewSessionRespDTO session) {
        ReviewDO reviewDO = new ReviewDO();
        reviewDO.setId(session.getId());
        reviewDO.setPrUrl(session.getPrUrl());
        reviewDO.setRepositoryFullName(session.getRepositoryFullName());
        reviewDO.setUserId(session.getUserId());
        reviewDO.setStatus(session.getStatus() == null ? null : session.getStatus().name());
        reviewDO.setPublishedCommentId(session.getPublishedCommentId());
        reviewDO.setCreatedAt(session.getCreatedAt());
        reviewDO.setCompletedAt(session.getCompletedAt());
        reviewDO.setUpdatedAt(Instant.now());
        reviewDO.setSnapshotJson(writeSnapshot(session));
        return reviewDO;
    }

    private ReviewSessionRespDTO toDTO(ReviewDO reviewDO) {
        if (reviewDO == null) {
            return null;
        }
        return readSnapshot(reviewDO.getSnapshotJson());
    }

    private String writeSnapshot(ReviewSessionRespDTO session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException ex) {
            throw new ServiceException("B500", "审查会话序列化失败");
        }
    }

    private ReviewSessionRespDTO readSnapshot(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson, ReviewSessionRespDTO.class);
        } catch (JsonProcessingException ex) {
            throw new ServiceException("B500", "审查会话反序列化失败");
        }
    }

    private String limitClause(int page, int size) {
        long offset = (long) page * size;
        return "LIMIT " + offset + "," + size;
    }
}

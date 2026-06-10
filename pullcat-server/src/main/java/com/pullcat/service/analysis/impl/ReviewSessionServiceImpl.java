package com.pullcat.service.analysis.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullcat.common.convention.exception.ServiceException;
import com.pullcat.dao.entity.RepoAutoPublishDO;
import com.pullcat.dao.entity.ReviewDO;
import com.pullcat.dao.mapper.RepoAutoPublishMapper;
import com.pullcat.dao.mapper.ReviewMapper;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.service.analysis.ReviewSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewSessionServiceImpl extends ServiceImpl<ReviewMapper, ReviewDO> implements ReviewSessionService {

    private final RepoAutoPublishMapper repoAutoPublishMapper;

    private final ObjectMapper objectMapper;

    @Override
    public void save(ReviewSessionRespDTO session) {
        ReviewDO reviewDO = toDO(session);
        if (baseMapper.selectById(session.getId()) == null) {
            baseMapper.insert(reviewDO);
        } else {
            baseMapper.updateById(reviewDO);
        }
    }

    @Override
    public ReviewSessionRespDTO findById(String id) {
        return toDTO(baseMapper.selectById(id));
    }

    @Override
    public List<ReviewSessionRespDTO> findAll(int page, int size) {
        return baseMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .orderByDesc(ReviewDO::getCreatedAt)
                        .last(limitClause(page, size)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<ReviewSessionRespDTO> findByRepo(String fullName, int page, int size) {
        return baseMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .eq(ReviewDO::getRepositoryFullName, fullName)
                        .orderByDesc(ReviewDO::getCreatedAt)
                        .last(limitClause(page, size)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<ReviewSessionRespDTO> findByLogin(String login, int page, int size) {
        return baseMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .eq(ReviewDO::getUserId, login)
                        .orderByDesc(ReviewDO::getCreatedAt)
                        .last(limitClause(page, size)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public long countByLogin(String login) {
        return baseMapper.selectCount(Wrappers.<ReviewDO>lambdaQuery()
                .eq(ReviewDO::getUserId, login));
    }

    @Override
    public List<ReviewSessionRespDTO> findAnonymous(int page, int size) {
        return baseMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .isNull(ReviewDO::getUserId)
                        .orderByDesc(ReviewDO::getCreatedAt)
                        .last(limitClause(page, size)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public long countAnonymous() {
        return baseMapper.selectCount(Wrappers.<ReviewDO>lambdaQuery()
                .isNull(ReviewDO::getUserId));
    }

    @Override
    public long count() {
        return baseMapper.selectCount(null);
    }

    @Override
    public long countByRepo(String fullName) {
        return baseMapper.selectCount(Wrappers.<ReviewDO>lambdaQuery()
                .eq(ReviewDO::getRepositoryFullName, fullName));
    }

    @Override
    public List<ReviewSessionRespDTO> findAllReviews() {
        return baseMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .orderByDesc(ReviewDO::getCreatedAt))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void delete(String id) {
        baseMapper.deleteById(id);
    }

    @Override
    public boolean exists(String id) {
        return baseMapper.selectById(id) != null;
    }

    @Override
    public boolean isAutoPublishEnabled(String owner, String repo) {
        RepoAutoPublishDO config = repoAutoPublishMapper.selectById(owner + "/" + repo);
        return config != null && config.isEnabled();
    }

    @Override
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

    @Override
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
        reviewDO.setInstallationId(session.getInstallationId());
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

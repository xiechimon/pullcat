package com.pullcat.service.analysis.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullcat.common.convention.exception.ServiceException;
import com.pullcat.dao.entity.ReviewDO;
import com.pullcat.dao.mapper.ReviewMapper;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.service.analysis.ReviewSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewSessionServiceImpl implements ReviewSessionService {

    private final ReviewMapper reviewMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void save(ReviewSessionRespDTO session) {
        ReviewDO reviewDO = toDO(session);
        if (reviewMapper.selectById(session.getId()) == null) {
            reviewMapper.insert(reviewDO);
        } else {
            reviewMapper.updateById(reviewDO);
        }
    }

    @Override
    public ReviewSessionRespDTO findById(String id) {
        return toDTO(reviewMapper.selectById(id));
    }

    @Override
    public List<ReviewSessionRespDTO> findAll(int page, int size) {
        return reviewMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .orderByDesc(ReviewDO::getCreatedAt)
                        .last(limitClause(page, size)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<ReviewSessionRespDTO> findByRepo(String fullName, int page, int size) {
        return reviewMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .eq(ReviewDO::getRepositoryFullName, fullName)
                        .orderByDesc(ReviewDO::getCreatedAt)
                        .last(limitClause(page, size)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<ReviewSessionRespDTO> findByLogin(String login, int page, int size) {
        return reviewMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .eq(ReviewDO::getUserId, login)
                        .orderByDesc(ReviewDO::getCreatedAt)
                        .last(limitClause(page, size)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public long countByLogin(String login) {
        return reviewMapper.selectCount(Wrappers.<ReviewDO>lambdaQuery()
                .eq(ReviewDO::getUserId, login));
    }

    @Override
    public List<ReviewSessionRespDTO> findAnonymous(int page, int size) {
        return reviewMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .isNull(ReviewDO::getUserId)
                        .orderByDesc(ReviewDO::getCreatedAt)
                        .last(limitClause(page, size)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public long countAnonymous() {
        return reviewMapper.selectCount(Wrappers.<ReviewDO>lambdaQuery()
                .isNull(ReviewDO::getUserId));
    }

    @Override
    public long count() {
        return reviewMapper.selectCount(null);
    }

    @Override
    public long countByRepo(String fullName) {
        return reviewMapper.selectCount(Wrappers.<ReviewDO>lambdaQuery()
                .eq(ReviewDO::getRepositoryFullName, fullName));
    }

    @Override
    public List<ReviewSessionRespDTO> findAllReviews() {
        return reviewMapper.selectList(Wrappers.<ReviewDO>lambdaQuery()
                        .orderByDesc(ReviewDO::getCreatedAt))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void delete(String id) {
        reviewMapper.deleteById(id);
    }

    @Override
    public boolean exists(String id) {
        return reviewMapper.selectById(id) != null;
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

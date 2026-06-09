package com.pullcat.service;

import com.pullcat.dto.resp.AutoPublishRepoRespDTO;
import com.pullcat.dto.resp.BooleanStatusRespDTO;
import com.pullcat.service.analysis.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 自动发布业务服务
 */
@Service
@RequiredArgsConstructor
public class AutoPublishService {

    private final ReviewRepository reviewRepository;

    /**
     * 查询所有已启用自动发布的仓库列表
     */
    public List<AutoPublishRepoRespDTO> listAutoPublishRepos() {
        return reviewRepository.listAutoPublishRepos().stream()
                .map(r -> {
                    String[] parts = r.split("/", 2);
                    if (parts.length < 2) return null;
                    return new AutoPublishRepoRespDTO(parts[0], parts[1], true);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 查询指定仓库的自动发布状态
     */
    public BooleanStatusRespDTO getStatus(String owner, String repo) {
        return new BooleanStatusRespDTO(reviewRepository.isAutoPublishEnabled(owner, repo));
    }

    /**
     * 设置指定仓库的自动发布状态
     */
    public BooleanStatusRespDTO setEnabled(String owner, String repo, Boolean enabled) {
        boolean value = Boolean.TRUE.equals(enabled);
        reviewRepository.setAutoPublishEnabled(owner, repo, value);
        return new BooleanStatusRespDTO(value);
    }
}

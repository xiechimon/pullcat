package com.pullcat.service.impl;

import com.pullcat.dto.resp.AutoPublishRepoRespDTO;
import com.pullcat.dto.resp.BooleanStatusRespDTO;
import com.pullcat.service.AutoPublishService;
import com.pullcat.service.analysis.ReviewSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AutoPublishServiceImpl implements AutoPublishService {

    private final ReviewSessionService reviewSessionService;

    @Override
    public List<AutoPublishRepoRespDTO> listAutoPublishRepos() {
        return reviewSessionService.listAutoPublishRepos().stream()
                .map(r -> {
                    String[] parts = r.split("/", 2);
                    if (parts.length < 2) return null;
                    return new AutoPublishRepoRespDTO(parts[0], parts[1], true);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public BooleanStatusRespDTO getStatus(String owner, String repo) {
        return new BooleanStatusRespDTO(reviewSessionService.isAutoPublishEnabled(owner, repo));
    }

    @Override
    public BooleanStatusRespDTO setEnabled(String owner, String repo, Boolean enabled) {
        boolean value = Boolean.TRUE.equals(enabled);
        reviewSessionService.setAutoPublishEnabled(owner, repo, value);
        return new BooleanStatusRespDTO(value);
    }
}

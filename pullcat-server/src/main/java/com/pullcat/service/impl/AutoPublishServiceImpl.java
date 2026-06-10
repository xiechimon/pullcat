package com.pullcat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pullcat.dao.entity.RepoAutoPublishDO;
import com.pullcat.dao.mapper.RepoAutoPublishMapper;
import com.pullcat.dto.resp.AutoPublishRepoRespDTO;
import com.pullcat.dto.resp.BooleanStatusRespDTO;
import com.pullcat.service.AutoPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoPublishServiceImpl implements AutoPublishService {

    private final RepoAutoPublishMapper repoAutoPublishMapper;

    @Override
    public List<AutoPublishRepoRespDTO> listAutoPublishRepos() {
        return repoAutoPublishMapper.selectList(new LambdaQueryWrapper<RepoAutoPublishDO>()
                        .eq(RepoAutoPublishDO::isEnabled, true)
                        .orderByAsc(RepoAutoPublishDO::getFullName))
                .stream()
                .map(r -> new AutoPublishRepoRespDTO(r.getOwner(), r.getRepo(), true))
                .toList();
    }

    @Override
    public BooleanStatusRespDTO getStatus(String owner, String repo) {
        RepoAutoPublishDO config = repoAutoPublishMapper.selectById(owner + "/" + repo);
        return new BooleanStatusRespDTO(config != null && config.isEnabled());
    }

    @Override
    public BooleanStatusRespDTO setEnabled(String owner, String repo, Boolean enabled) {
        boolean value = Boolean.TRUE.equals(enabled);
        String fullName = owner + "/" + repo;
        if (value) {
            RepoAutoPublishDO existing = repoAutoPublishMapper.selectById(fullName);
            RepoAutoPublishDO config = new RepoAutoPublishDO(owner, repo, true);
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
        return new BooleanStatusRespDTO(value);
    }
}

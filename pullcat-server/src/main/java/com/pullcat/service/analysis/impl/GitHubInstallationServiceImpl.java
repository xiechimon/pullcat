package com.pullcat.service.analysis.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pullcat.dao.entity.GitHubInstallationDO;
import com.pullcat.dao.entity.UserDO;
import com.pullcat.dao.mapper.GitHubInstallationMapper;
import com.pullcat.dao.mapper.UserMapper;
import com.pullcat.service.analysis.GitHubInstallationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * GitHub App 安装记录服务实现
 */
@Service
@RequiredArgsConstructor
public class GitHubInstallationServiceImpl implements GitHubInstallationService {

    private final GitHubInstallationMapper gitHubInstallationMapper;
    private final UserMapper userMapper;

    @Override
    public void saveInstallation(long installationId, String accountLogin, String accountType) {
        GitHubInstallationDO record = gitHubInstallationMapper.selectById(installationId);
        Instant now = Instant.now();
        if (record == null) {
            record = new GitHubInstallationDO();
            record.setInstallationId(installationId);
            record.setInstalledAt(now);
        }
        record.setAccountLogin(accountLogin);
        record.setAccountType(accountType);
        record.setSuspendedAt(null);
        if (gitHubInstallationMapper.selectById(installationId) == null) {
            gitHubInstallationMapper.insert(record);
        } else {
            gitHubInstallationMapper.updateById(record);
        }

        if ("User".equals(accountType) && accountLogin != null && !accountLogin.isBlank()) {
            UserDO user = userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                    .eq(UserDO::getGithubLogin, accountLogin));
            if (user != null) {
                user.setInstallationId(installationId);
                userMapper.updateById(user);
            }
        }
    }

    @Override
    public void suspendInstallation(long installationId) {
        GitHubInstallationDO record = gitHubInstallationMapper.selectById(installationId);
        if (record == null) {
            return;
        }
        record.setSuspendedAt(Instant.now());
        gitHubInstallationMapper.updateById(record);
    }

    @Override
    public Optional<Long> findInstallationIdByLogin(String login) {
        GitHubInstallationDO record = gitHubInstallationMapper.selectOne(new LambdaQueryWrapper<GitHubInstallationDO>()
                .eq(GitHubInstallationDO::getAccountLogin, login)
                .isNull(GitHubInstallationDO::getSuspendedAt)
                .last("LIMIT 1"));
        return Optional.ofNullable(record).map(GitHubInstallationDO::getInstallationId);
    }
}

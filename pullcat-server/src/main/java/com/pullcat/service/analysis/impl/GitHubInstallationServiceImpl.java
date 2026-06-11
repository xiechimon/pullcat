package com.pullcat.service.analysis.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pullcat.dao.entity.GitHubInstallationDO;
import com.pullcat.dao.entity.UserDO;
import com.pullcat.dao.mapper.GitHubInstallationMapper;
import com.pullcat.dao.mapper.UserMapper;
import com.pullcat.remote.GitHubInstallationTokenService;
import com.pullcat.service.analysis.GitHubInstallationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * GitHub App 安装记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubInstallationServiceImpl extends ServiceImpl<GitHubInstallationMapper, GitHubInstallationDO> implements GitHubInstallationService {

    private final UserMapper userMapper;
    private final GitHubInstallationTokenService gitHubInstallationTokenService;

    @Override
    public void saveInstallation(long installationId, String accountLogin, String accountType) {
        if (accountLogin == null || accountLogin.isBlank()) {
            try {
                GitHubInstallationTokenService.InstallationAccount account = gitHubInstallationTokenService
                        .getInstallationAccount(installationId).block();
                if (account != null) {
                    accountLogin = account.login();
                    accountType = account.type();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch installation account for {}: {}", installationId, e.getMessage());
            }
        }

        GitHubInstallationDO record = baseMapper.selectById(installationId);
        Instant now = Instant.now();
        if (record == null) {
            record = new GitHubInstallationDO();
            record.setInstallationId(installationId);
            record.setInstalledAt(now);
        }
        record.setAccountLogin(accountLogin);
        record.setAccountType(accountType);
        record.setSuspendedAt(null);
        if (baseMapper.selectById(installationId) == null) {
            baseMapper.insert(record);
        } else {
            baseMapper.updateById(record);
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
        GitHubInstallationDO record = baseMapper.selectById(installationId);
        if (record == null) {
            return;
        }
        record.setSuspendedAt(Instant.now());
        baseMapper.updateById(record);
    }

    @Override
    public Optional<Long> findInstallationIdByLogin(String login) {
        GitHubInstallationDO record = baseMapper.selectOne(new LambdaQueryWrapper<GitHubInstallationDO>()
                .eq(GitHubInstallationDO::getAccountLogin, login)
                .isNull(GitHubInstallationDO::getSuspendedAt)
                .last("LIMIT 1"));
        return Optional.ofNullable(record).map(GitHubInstallationDO::getInstallationId);
    }
}

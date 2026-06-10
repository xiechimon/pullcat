package com.pullcat.service.analysis;

import java.util.Optional;

/**
 * GitHub App 安装记录服务
 */
public interface GitHubInstallationService {

    /**
     * 保存或更新安装记录
     */
    void saveInstallation(long installationId, String accountLogin, String accountType);

    /**
     * 标记安装已暂停或卸载
     */
    void suspendInstallation(long installationId);

    /**
     * 根据登录名查找安装 ID
     */
    Optional<Long> findInstallationIdByLogin(String login);
}

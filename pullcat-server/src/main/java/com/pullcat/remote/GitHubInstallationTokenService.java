package com.pullcat.remote;

import reactor.core.publisher.Mono;

/**
 * GitHub App Installation Token 服务，通过 App JWT 换取 Installation Token 并缓存
 */
public interface GitHubInstallationTokenService {

    /**
     * 获取指定安装 ID 的 Installation Token，优先从 Redis 缓存读取
     */
    Mono<String> getInstallationToken(long installationId);

    /**
     * 通过 JWT 获取安装账号信息（login + type），用于 setup 回调缺少账号参数时补充
     */
    Mono<InstallationAccount> getInstallationAccount(long installationId);

    record InstallationAccount(String login, String type) {}
}

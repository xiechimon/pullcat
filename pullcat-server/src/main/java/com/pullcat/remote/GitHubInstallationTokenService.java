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
}

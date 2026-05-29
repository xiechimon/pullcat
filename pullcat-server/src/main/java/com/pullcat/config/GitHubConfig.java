package com.pullcat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GitHub 配置类，通过 {@code @ConfigurationProperties} 绑定 {@code pullcat.github} 前缀的配置项，
 * 用于管理 GitHub API 访问令牌。
 */
@Configuration
@ConfigurationProperties(prefix = "pullcat.github")
public class GitHubConfig {

    private String token;

    /**
     * 获取 GitHub 个人访问令牌
     *
     * @return GitHub API 认证令牌
     */
    public String getToken() { return token; }

    /**
     * 设置 GitHub 个人访问令牌
     *
     * @param token GitHub API 认证令牌
     */
    public void setToken(String token) { this.token = token; }
}

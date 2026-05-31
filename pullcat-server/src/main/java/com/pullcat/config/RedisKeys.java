package com.pullcat.config;

import java.time.Duration;

/**
 * Redis 键名常量与配置，统一管理所有 Redis key 前缀、索引键名和过期时间。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /**
     * Review 会话过期时间（7天）
     */
    public static final Duration REVIEW_TTL = Duration.ofDays(7);

    /**
     * Review 会话 key 前缀，完整 key 格式: review:{id}
     */
    public static final String REVIEW_PREFIX = "review:";

    /**
     * Review 会话 ID 索引（按时间排序的 sorted set）
     */
    public static final String REVIEW_INDEX = "review:index";

    /**
     * Review 按仓库索引前缀，格式: review:repo:{owner}/{repo}
     */
    public static final String REVIEW_REPO_PREFIX = "review:repo:";

    /**
     * Repo 收藏 key 前缀，完整 key 格式: repo:{owner}/{repo}
     */
    public static final String REPO_PREFIX = "repo:";

    /**
     * Repo 收藏 ID 索引（set）
     */
    public static final String REPO_INDEX = "repo:index";

    /**
     * 根据 ID 构造 Review key
     */
    public static String reviewKey(String id) {
        return REVIEW_PREFIX + id;
    }

    /**
     * 根据 owner/repo 构造 Review 仓库索引 key
     */
    public static String reviewRepoKey(String owner, String repo) {
        return REVIEW_REPO_PREFIX + owner + "/" + repo;
    }

    /**
     * 根据 fullName 构造 Repo key
     */
    public static String repoKey(String fullName) {
        return REPO_PREFIX + fullName;
    }

    /**
     * Rule 建议缓存 key 前缀，格式: rule-suggestions:{owner}/{repo}
     */
    public static final String RULE_SUGGESTIONS_PREFIX = "rule-suggestions:";

    /**
     * Review 按用户索引前缀，格式: review:user:{login}
     */
    public static final String REVIEW_USER_INDEX_PREFIX = "review:user:";

    /**
     * Review 匿名用户索引
     */
    public static final String REVIEW_ANONYMOUS_INDEX = "review:anonymous";

    public static String reviewUserKey(String login) {
        return REVIEW_USER_INDEX_PREFIX + login;
    }
}

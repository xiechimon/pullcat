package com.pullcat.service.github;

/**
 * GitHub API 返回 403 Forbidden 时抛出。
 * 通常表示 token 缺少所需 scope（如 public_repo）。
 */
public class GitHubForbiddenException extends RuntimeException {

    public GitHubForbiddenException(String message) {
        super(message);
    }

    public GitHubForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}

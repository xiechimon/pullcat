package com.pullcat.controller;

import com.pullcat.service.github.GitHubForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GitHubForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleGitHubForbidden(GitHubForbiddenException ex) {
        log.warn("GitHub API 403: {}", ex.getMessage());
        String detail = ex.getMessage();
        String message;

        if (detail != null && detail.contains("OAuth App access restrictions")) {
            message = "该组织已启用 OAuth App 访问限制，第三方应用需要组织管理员批准后才能访问。" +
                    "请在 GitHub 组织的 Settings > Third-party access 中批准本 OAuth App，" +
                    "或使用具有该组织访问权限的 Personal Access Token 替代。";
        } else {
            message = "请检查 GitHub token 是否具有 public_repo 权限。" +
                    "在 GitHub OAuth App 设置中确保已勾选 public_repo scope，" +
                    "或更新 GITHUB_TOKEN 环境变量为具有 repo/public_repo 权限的 Personal Access Token。";
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "GitHub API 权限不足",
                        "message", message,
                        "detail", detail));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal Server Error",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Unexpected error"));
    }
}

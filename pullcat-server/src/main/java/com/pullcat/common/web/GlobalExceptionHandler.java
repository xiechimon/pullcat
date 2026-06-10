package com.pullcat.common.web;

import com.pullcat.common.convention.exception.AbstractBusinessException;
import com.pullcat.common.convention.exception.GitHubForbiddenException;
import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(HttpServletRequest request, MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : CommonErrorCodeEnum.VALIDATION_ERROR.message();
        log.warn("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL(), message);
        return Results.failure(CommonErrorCodeEnum.VALIDATION_ERROR, message);
    }

    /**
     * GitHub API 返回 403 时，根据消息关键字提供具体中文指引
     */
    @ExceptionHandler(GitHubForbiddenException.class)
    public Result<Void> handleGitHubForbidden(HttpServletRequest request, GitHubForbiddenException ex) {
        String detail = ex.getMessage();
        String message;

        if (detail != null && detail.contains("secondary rate limit")) {
            message = "触发 GitHub 次级限流，已被暂时限制内容创建。请等待几分钟后重试。频繁发布可能会延长限制时间。";
        } else if (detail != null && detail.contains("OAuth App access restrictions")) {
            message = "该组织已启用 OAuth App 访问限制，第三方应用需要组织管理员批准后才能访问。" +
                    "请在 GitHub 组织的 Settings > Third-party access 中批准本 OAuth App，" +
                    "或使用具有该组织访问权限的 Personal Access Token 替代。";
        } else if (detail != null && detail.contains("Resource not accessible by personal access token")) {
            message = "当前使用的 Personal Access Token 没有访问该仓库的权限。" +
                    "请检查 GITHUB_TOKEN 是否为 fine-grained token 且未配置仓库权限，" +
                    "或前往 https://github.com/settings/tokens 为 token 添加 Pull requests 读写权限。";
        } else {
            message = "请检查 GitHub token 是否具有 public_repo 权限。" +
                    "在 GitHub OAuth App 设置中确保已勾选 public_repo scope，" +
                    "或更新 GITHUB_TOKEN 环境变量为具有 repo/public_repo 权限的 Personal Access Token。";
        }

        String fullMessage = message + " detail=" + Objects.requireNonNullElse(detail, "");
        log.warn("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL(), fullMessage);
        return Results.failure(CommonErrorCodeEnum.FORBIDDEN, fullMessage);
    }

    @ExceptionHandler(AbstractBusinessException.class)
    public Result<Void> handleBusinessException(HttpServletRequest request, AbstractBusinessException ex) {
        log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL(), ex.getMessage());
        return Results.failure(ex);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<Void>> handleResponseStatus(HttpServletRequest request, ResponseStatusException ex) {
        log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL(), ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode())
                .body(Results.failure(String.valueOf(ex.getStatusCode().value()), ex.getReason()));
    }

    /**
     * 客户端断开 SSE 长连接时抛出，返回 void 避免触发二次写响应异常
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncNotUsable(AsyncRequestNotUsableException ex) {
        log.debug("客户端已断开 SSE 连接: {}", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResourceFound() {
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleGeneral(HttpServletRequest request, Exception ex) {
        log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL(), ex.getMessage(), ex);
        return Results.failure(CommonErrorCodeEnum.SERVICE_ERROR,
                ex.getMessage() != null ? ex.getMessage() : CommonErrorCodeEnum.SERVICE_ERROR.message());
    }
}

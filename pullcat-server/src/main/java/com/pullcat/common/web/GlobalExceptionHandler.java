package com.pullcat.common.web;

import com.pullcat.common.convention.exception.AbstractBusinessException;
import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.convention.exception.GitHubForbiddenException;
import com.pullcat.common.convention.exception.RemoteException;
import com.pullcat.common.convention.exception.ServiceException;
import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;

/**
 * 全局异常处理器，统一将各类异常转换为结构化的 HTTP 响应。
 * <p>
 * 使用 {@link RestControllerAdvice} 拦截 Controller 层抛出的异常，
 * 避免异常直接暴露给客户端，同时提供可读的中文错误提示。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 GitHub API 返回 403 Forbidden 的场景。
     * <p>
     * 根据异常消息中的关键字区分具体原因并返回对应的中文指引：
     * <ul>
     *   <li>次级限流（secondary rate limit）—— 提示用户等待后重试</li>
     *   <li>OAuth App 访问限制 —— 指引用户在组织设置中批准应用或切换 Token</li>
     *   <li>Personal Access Token 无仓库权限 —— 指引用户检查 Token 类型及权限配置</li>
     *   <li>其他 403 错误 —— 提示检查 public_repo 权限</li>
     * </ul>
     *
     * @param ex GitHubForbiddenException，其 message 为 GitHub API 返回的错误详情
     * @return 403 响应，包含 error/message/detail 三个字段的 Map
     */
    @ExceptionHandler(GitHubForbiddenException.class)
    public ResponseEntity<Result<Void>> handleGitHubForbidden(GitHubForbiddenException ex) {
        log.warn("GitHub API 403: {}", ex.getMessage());
        String detail = ex.getMessage();
        String message;

        if (detail != null && detail.contains("secondary rate limit")) {
            message = "触发 GitHub 次级限流，已被暂时限制内容创建。请等待几分钟后重试。" +
                    "频繁发布可能会延长限制时间。";
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

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Results.failure(CommonErrorCodeEnum.FORBIDDEN,
                        message + " detail=" + Objects.requireNonNullElse(detail, "")));
    }

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<Result<Void>> handleClientException(ClientException ex) {
        log.warn("Client exception: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Results.failure(ex));
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Result<Void>> handleServiceException(ServiceException ex) {
        log.error("Service exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Results.failure(ex));
    }

    @ExceptionHandler(RemoteException.class)
    public ResponseEntity<Result<Void>> handleRemoteException(RemoteException ex) {
        log.warn("Remote exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Results.failure(ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : CommonErrorCodeEnum.VALIDATION_ERROR.message();
        return ResponseEntity.badRequest().body(Results.failure(CommonErrorCodeEnum.VALIDATION_ERROR, message));
    }

    /**
     * 处理客户端断开 SSE 长连接时抛出的异步请求不可用异常。
     * <p>
     * 返回 void 以避免 Spring 在尝试写响应时再次抛出
     * {@link org.springframework.http.converter.HttpMessageNotWritableException}。
     *
     * @param ex AsyncRequestNotUsableException，通常因客户端主动断开连接触发
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException ex) {
        log.debug("客户端已断开 SSE 连接: {}", ex.getMessage());
    }

    /**
     * 处理请求不存在的静态资源（如 /favicon.ico）时 Spring 抛出的异常。
     *
     * @return 404 空响应体
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound() {
        return ResponseEntity.notFound().build();
    }

    /**
     * 兜底异常处理，捕获所有未被上述处理器匹配的异常。
     * <p>
     * 记录完整堆栈日志，返回 500 响应并携带异常消息（避免将敏感堆栈信息暴露给客户端）。
     *
     * @param ex 未被其他处理器捕获的异常
     * @return 500 响应，包含 error 和 message 字段的 Map
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Results.failure(CommonErrorCodeEnum.SERVICE_ERROR,
                        ex.getMessage() != null ? ex.getMessage() : CommonErrorCodeEnum.SERVICE_ERROR.message()));
    }
}

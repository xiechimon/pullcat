package com.pullcat.common.enums;

import com.pullcat.common.convention.errorcode.IErrorCode;

/**
 * 通用错误码枚举，为尚未细分业务模块时提供默认错误码。
 */
public enum CommonErrorCodeEnum implements IErrorCode {

    SUCCESS("0", "success"),
    CLIENT_ERROR("A000001", "客户端请求错误"),
    UNAUTHORIZED("A000401", "未授权访问"),
    FORBIDDEN("A000403", "无权访问"),
    NOT_FOUND("A000404", "资源不存在"),
    VALIDATION_ERROR("A000422", "请求参数校验失败"),
    SERVICE_ERROR("B000001", "服务内部错误"),
    REMOTE_ERROR("C000001", "远程调用失败");

    private final String code;
    private final String message;

    CommonErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}

package com.pullcat.common.convention.result;

import com.pullcat.common.convention.errorcode.IErrorCode;
import com.pullcat.common.convention.exception.AbstractBusinessException;
import com.pullcat.common.enums.CommonErrorCodeEnum;

/**
 * 统一响应工厂方法。
 */
public final class Results {

    private Results() {
    }

    public static Result<Void> success() {
        return Result.of(true, CommonErrorCodeEnum.SUCCESS.code(), CommonErrorCodeEnum.SUCCESS.message(), null);
    }

    public static <T> Result<T> success(T data) {
        return Result.of(true, CommonErrorCodeEnum.SUCCESS.code(), CommonErrorCodeEnum.SUCCESS.message(), data);
    }

    public static Result<Void> failure(IErrorCode errorCode) {
        return Result.of(false, errorCode.code(), errorCode.message(), null);
    }

    public static Result<Void> failure(IErrorCode errorCode, String message) {
        return Result.of(false, errorCode.code(), message, null);
    }

    public static Result<Void> failure(AbstractBusinessException exception) {
        return Result.of(false, exception.getErrorCode(), exception.getMessage(), null);
    }

    public static Result<Void> failure(String code, String message) {
        return Result.of(false, code, message, null);
    }
}

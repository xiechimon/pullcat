package com.pullcat.common.convention.exception;

import com.pullcat.common.convention.errorcode.IErrorCode;

/**
 * 业务异常基类，统一承载错误码与消息。
 */
public abstract class AbstractBusinessException extends RuntimeException {

    private final String errorCode;

    protected AbstractBusinessException(IErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode.code();
    }

    protected AbstractBusinessException(IErrorCode errorCode, Throwable cause) {
        super(errorCode.message(), cause);
        this.errorCode = errorCode.code();
    }

    protected AbstractBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected AbstractBusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

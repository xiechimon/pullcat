package com.pullcat.common.convention.exception;

import com.pullcat.common.convention.errorcode.IErrorCode;

/**
 * 客户端异常，表示参数、权限或业务前置条件错误
 */
public class ClientException extends AbstractBusinessException {

    public ClientException(IErrorCode errorCode) {
        super(errorCode);
    }

    public ClientException(IErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ClientException(String errorCode, String message) {
        super(errorCode, message);
    }
}

package com.pullcat.common.convention.exception;

import com.pullcat.common.convention.errorcode.IErrorCode;

/**
 * 服务端异常，表示系统内部执行失败。
 */
public class ServiceException extends AbstractBusinessException {

    public ServiceException(IErrorCode errorCode) {
        super(errorCode);
    }

    public ServiceException(IErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ServiceException(String errorCode, String message) {
        super(errorCode, message);
    }
}

package com.pullcat.common.convention.exception;

import com.pullcat.common.convention.errorcode.IErrorCode;

/**
 * 远程调用异常，表示第三方服务或下游服务调用失败。
 */
public class RemoteException extends AbstractBusinessException {

    public RemoteException(IErrorCode errorCode) {
        super(errorCode);
    }

    public RemoteException(IErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public RemoteException(String errorCode, String message) {
        super(errorCode, message);
    }
}

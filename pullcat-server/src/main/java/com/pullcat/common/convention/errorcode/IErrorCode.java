package com.pullcat.common.convention.errorcode;

/**
 * 错误码协议，约束错误码与错误消息的最小结构。
 */
public interface IErrorCode {

    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    String code();

    /**
     * 获取错误消息。
     *
     * @return 错误消息
     */
    String message();
}

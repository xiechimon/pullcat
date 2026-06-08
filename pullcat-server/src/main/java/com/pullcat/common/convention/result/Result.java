package com.pullcat.common.convention.result;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一响应对象。
 *
 * @param <T> 响应数据类型
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Result<T> {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 响应码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    public static <T> Result<T> of(boolean success, String code, String message, T data) {
        return new Result<>(success, code, message, data);
    }
}

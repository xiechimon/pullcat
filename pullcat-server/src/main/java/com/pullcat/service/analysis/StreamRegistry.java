package com.pullcat.service.analysis;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 流注册中心，使用静态并发映射管理当前活跃的 SSE 连接。
 */
public final class StreamRegistry {

    private static final ConcurrentHashMap<String, StreamContext> MAP = new ConcurrentHashMap<>();

    private StreamRegistry() {
    }

    /**
     * 注册 SSE 流上下文。
     *
     * @param reviewId 审查会话 ID
     * @param ctx      SSE 流上下文
     */
    public static void register(String reviewId, StreamContext ctx) {
        MAP.put(reviewId, ctx);
    }

    /**
     * 获取已注册的 SSE 流上下文。
     *
     * @param reviewId 审查会话 ID
     * @return SSE 流上下文，不存在时返回 null
     */
    public static StreamContext get(String reviewId) {
        return MAP.get(reviewId);
    }

    /**
     * 移除已注册的 SSE 流上下文。
     *
     * @param reviewId 审查会话 ID
     */
    public static void remove(String reviewId) {
        MAP.remove(reviewId);
    }
}

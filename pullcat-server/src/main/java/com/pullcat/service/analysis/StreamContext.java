package com.pullcat.service.analysis;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 流上下文，关联审查 ID 与对应的 SSE 发射器。
 */
public record StreamContext(String reviewId, SseEmitter emitter) {
}

package com.pullcat.service.analysis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimiter {

    private static final String KEY_PREFIX = "rate:v2:";

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, int maxRequests, Duration window) {
        String redisKey = KEY_PREFIX + key;
        Long current = redisTemplate.opsForValue().increment(redisKey);
        if (current == null) return false;

        if (current == 1) {
            redisTemplate.expire(redisKey, window);
        } else {
            // 防御性编程：如果因为异常导致 TTL 没有设置成功（TTL 为 -1），则重新设置
            Long expire = redisTemplate.getExpire(redisKey);
            if (expire != null && expire == -1) {
                redisTemplate.expire(redisKey, window);
            }
        }

        return current <= maxRequests;
    }

    public long getRemaining(String key, int maxRequests) {
        String redisKey = KEY_PREFIX + key;
        String value = redisTemplate.opsForValue().get(redisKey);
        if (value == null) return maxRequests;
        long current = Long.parseLong(value);
        return Math.max(0, maxRequests - current);
    }
}

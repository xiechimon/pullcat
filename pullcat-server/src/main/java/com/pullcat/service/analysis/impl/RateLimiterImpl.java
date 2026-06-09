package com.pullcat.service.analysis.impl;

import com.pullcat.service.analysis.RateLimiter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimiterImpl implements RateLimiter {

    private static final String KEY_PREFIX = "rate:v2:";

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimiterImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAllowed(String key, int maxRequests, Duration window) {
        String redisKey = KEY_PREFIX + key;
        Long current = redisTemplate.opsForValue().increment(redisKey);
        if (current == null) {
            return false;
        }

        if (current == 1) {
            redisTemplate.expire(redisKey, window);
        } else {
            Long expire = redisTemplate.getExpire(redisKey);
            if (expire != null && expire == -1) {
                redisTemplate.expire(redisKey, window);
            }
        }

        return current <= maxRequests;
    }

    @Override
    public long getRemaining(String key, int maxRequests) {
        String redisKey = KEY_PREFIX + key;
        String value = redisTemplate.opsForValue().get(redisKey);
        if (value == null) {
            return maxRequests;
        }
        long current = Long.parseLong(value);
        return Math.max(0, maxRequests - current);
    }
}

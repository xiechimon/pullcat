package com.pullcat.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory connectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try {
            RedisConnection connection = connectionFactory.getConnection();
            String pong = connection.ping();
            connection.close();
            if ("PONG".equals(pong)) {
                return Health.up().withDetail("redis", "connected").build();
            }
            return Health.down().withDetail("redis", "unexpected ping response: " + pong).build();
        } catch (Exception e) {
            return Health.down(e).withDetail("redis", "connection failed").build();
        }
    }
}

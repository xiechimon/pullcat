package com.pullcat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类，用于注册自定义 ObjectMapper 与 RedisTemplate Bean。
 */
@Configuration
public class RedisConfig {

    /**
     * 创建兼容 Java 8 时间类型的 ObjectMapper Bean。
     *
     * @return 注册了 JavaTimeModule 的 ObjectMapper 实例
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * 创建 RedisTemplate Bean，使用 JSON 序列化值、字符串序列化键。
     *
     * @param factory      Redis 连接工厂
     * @param objectMapper 自定义 ObjectMapper
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();

        return template;
    }
}

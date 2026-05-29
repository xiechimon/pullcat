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
 * Redis 配置类，负责创建 RedisTemplate 及自定义序列化所需的 ObjectMapper Bean。
 * 使用 JSON 格式序列化值，Key 采用字符串序列化。
 */
@Configuration
public class RedisConfig {

    /**
     * 创建 Redis 专用的 ObjectMapper Bean，注册 Java 8 时间模块以支持 LocalDateTime 等类型。
     *
     * @return 配置了 JavaTimeModule 的 ObjectMapper 实例
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * 配置 RedisTemplate，使用 JSON 序列化器存储对象，Key 使用字符串序列化。
     *
     * @param factory      Redis 连接工厂
     * @param objectMapper 用于 JSON 序列化的 ObjectMapper（由 {@link #redisObjectMapper()} 提供）
     * @return 配置完成的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory, ObjectMapper objectMapper) {

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}

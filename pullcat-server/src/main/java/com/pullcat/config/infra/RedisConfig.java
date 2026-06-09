package com.pullcat.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.NonNull;

/**
 * Redis 配置类
 */
@Configuration
public class RedisConfig {

    /**
     * 创建 RedisTemplate bean，配置 Key 和 Value 的序列化策略。
     *
     * @param factory Redis 连接工厂
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        RedisTemplate<String, Object> template = getStringObjectRedisTemplate(factory, mapper);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * 构建 RedisTemplate 实例并配置所有序列化器。
     *
     * @param factory Redis 连接工厂，提供到 Redis 服务器的连接
     * @param mapper  已配置好多态支持和时间模块的 ObjectMapper 实例
     * @return 完全配置的 RedisTemplate 实例
     */
    @NonNull
    private static RedisTemplate<String, Object> getStringObjectRedisTemplate(RedisConnectionFactory factory, ObjectMapper mapper) {
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(mapper);
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        return template;
    }
}

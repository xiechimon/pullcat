package com.pullcat.service.analysis;

import com.pullcat.model.ReviewSession;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 审查会话存储仓库，基于 Redis 实现持久化，支持 7 天过期。
 */
@Repository
public class ReviewRepository {

    private static final String KEY_PREFIX = "review:";
    private static final Duration TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;

    public ReviewRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String id) {
        return KEY_PREFIX + id;
    }

    /**
     * 保存审查会话到 Redis，设置 7 天过期时间。
     *
     * @param session 审查会话对象
     */
    public void save(ReviewSession session) {
        redisTemplate.opsForValue().set(key(session.getId()), session, TTL);
    }

    /**
     * 根据 ID 查询审查会话。
     *
     * @param id 会话唯一标识
     * @return 审查会话对象，不存在时返回 null
     */
    public ReviewSession findById(String id) {
        Object obj = redisTemplate.opsForValue().get(key(id));
        if (obj instanceof ReviewSession) {
            return (ReviewSession) obj;
        }
        return null;
    }

    /**
     * 根据 ID 删除审查会话。
     *
     * @param id 会话唯一标识
     */
    public void delete(String id) {
        redisTemplate.delete(key(id));
    }

    /**
     * 检查指定 ID 的审查会话是否存在。
     *
     * @param id 会话唯一标识
     * @return 存在返回 true，否则返回 false
     */
    public boolean exists(String id) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(id)));
    }
}

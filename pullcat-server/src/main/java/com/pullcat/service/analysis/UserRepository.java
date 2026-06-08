package com.pullcat.service.analysis;

import com.pullcat.dao.entity.UserDO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final String KEY_PREFIX = "user:";

    private final RedisTemplate<String, Object> redisTemplate;

    public UserRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(UserDO user) {
        redisTemplate.opsForValue().set(KEY_PREFIX + user.getId(), user);
        redisTemplate.opsForValue().set(KEY_PREFIX + "login:" + user.getGithubLogin(), user.getId());
    }

    public UserDO findById(String id) {
        Object obj = redisTemplate.opsForValue().get(KEY_PREFIX + id);
        return obj instanceof UserDO ? (UserDO) obj : null;
    }

    public UserDO findByLogin(String login) {
        Object id = redisTemplate.opsForValue().get(KEY_PREFIX + "login:" + login);
        if (id == null) return null;
        return findById(id.toString());
    }
}

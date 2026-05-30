package com.pullcat.service.analysis;

import com.pullcat.model.ReviewSession;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class ReviewRepository {

    private static final String KEY_PREFIX = "review:";
    private static final String INDEX_KEY = "review:index";
    private static final String REPO_INDEX_PREFIX = "review:repo:";
    private static final Duration TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;

    public ReviewRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String id) {
        return KEY_PREFIX + id;
    }

    public void save(ReviewSession session) {
        redisTemplate.opsForValue().set(key(session.getId()), session, TTL);
        redisTemplate.opsForZSet().add(INDEX_KEY, session.getId(), session.getCreatedAt().toEpochMilli());

        if (session.getRepositoryFullName() != null) {
            redisTemplate.opsForZSet().add(REPO_INDEX_PREFIX + session.getRepositoryFullName(),
                    session.getId(), session.getCreatedAt().toEpochMilli());
        }
    }

    public ReviewSession findById(String id) {
        Object obj = redisTemplate.opsForValue().get(key(id));
        if (obj instanceof ReviewSession) {
            return (ReviewSession) obj;
        }
        return null;
    }

    public List<ReviewSession> findAll(int page, int size) {
        long start = (long) page * size;
        long end = start + size - 1;

        Set<Object> ids = redisTemplate.opsForZSet().reverseRange(INDEX_KEY, start, end);
        return fetchByIds(ids);
    }

    public List<ReviewSession> findByRepo(String fullName, int page, int size) {
        long start = (long) page * size;
        long end = start + size - 1;

        Set<Object> ids = redisTemplate.opsForZSet().reverseRange(REPO_INDEX_PREFIX + fullName, start, end);
        return fetchByIds(ids);
    }

    public long count() {
        Long size = redisTemplate.opsForZSet().size(INDEX_KEY);
        return size != null ? size : 0;
    }

    public long countByRepo(String fullName) {
        Long size = redisTemplate.opsForZSet().size(REPO_INDEX_PREFIX + fullName);
        return size != null ? size : 0;
    }

    public List<ReviewSession> findAllReviews() {
        Set<Object> ids = redisTemplate.opsForZSet().reverseRange(INDEX_KEY, 0, -1);
        return fetchByIds(ids);
    }

    private List<ReviewSession> fetchByIds(Set<Object> ids) {
        List<ReviewSession> results = new ArrayList<>();
        if (ids == null || ids.isEmpty()) return results;

        for (Object id : ids) {
            ReviewSession session = findById(id.toString());
            if (session != null) {
                results.add(session);
            }
        }
        return results;
    }

    public void delete(String id) {
        ReviewSession session = findById(id);
        if (session != null && session.getRepositoryFullName() != null) {
            redisTemplate.opsForZSet().remove(REPO_INDEX_PREFIX + session.getRepositoryFullName(), id);
        }
        redisTemplate.opsForZSet().remove(INDEX_KEY, id);
        redisTemplate.delete(key(id));
    }

    public boolean exists(String id) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(id)));
    }
}

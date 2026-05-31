package com.pullcat.service.analysis;

import com.pullcat.config.RedisKeys;
import com.pullcat.model.ReviewSession;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
public class ReviewRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    public ReviewRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(ReviewSession session) {
        redisTemplate.opsForValue().set(RedisKeys.reviewKey(session.getId()), session, RedisKeys.REVIEW_TTL);
        redisTemplate.opsForZSet().add(RedisKeys.REVIEW_INDEX, session.getId(), session.getCreatedAt().toEpochMilli());

        if (session.getRepositoryFullName() != null) {
            String[] parts = session.getRepositoryFullName().split("/", 2);
            redisTemplate.opsForZSet().add(RedisKeys.reviewRepoKey(parts[0], parts[1]),
                    session.getId(), session.getCreatedAt().toEpochMilli());
        }

        if (session.getUserId() != null) {
            redisTemplate.opsForZSet().add(RedisKeys.reviewUserKey(session.getUserId()),
                    session.getId(), session.getCreatedAt().toEpochMilli());
        } else {
            redisTemplate.opsForZSet().add(RedisKeys.REVIEW_ANONYMOUS_INDEX,
                    session.getId(), session.getCreatedAt().toEpochMilli());
        }
    }

    public ReviewSession findById(String id) {
        Object obj = redisTemplate.opsForValue().get(RedisKeys.reviewKey(id));
        if (obj instanceof ReviewSession) {
            return (ReviewSession) obj;
        }
        return null;
    }

    public List<ReviewSession> findAll(int page, int size) {
        long start = (long) page * size;
        long end = start + size - 1;

        Set<Object> ids = redisTemplate.opsForZSet().reverseRange(RedisKeys.REVIEW_INDEX, start, end);
        return fetchByIds(ids);
    }

    public List<ReviewSession> findByRepo(String fullName, int page, int size) {
        long start = (long) page * size;
        long end = start + size - 1;

        String[] parts = fullName.split("/", 2);
        Set<Object> ids = redisTemplate.opsForZSet().reverseRange(
                RedisKeys.reviewRepoKey(parts[0], parts[1]), start, end);
        return fetchByIds(ids);
    }

    public List<ReviewSession> findByLogin(String login, int page, int size) {
        long start = (long) page * size;
        long end = start + size - 1;
        Set<Object> ids = redisTemplate.opsForZSet().reverseRange(RedisKeys.reviewUserKey(login), start, end);
        return fetchByIds(ids);
    }

    public long countByLogin(String login) {
        Long s = redisTemplate.opsForZSet().size(RedisKeys.reviewUserKey(login));
        return s != null ? s : 0;
    }

    public List<ReviewSession> findAnonymous(int page, int size) {
        long start = (long) page * size;
        long end = start + size - 1;
        Set<Object> ids = redisTemplate.opsForZSet().reverseRange(RedisKeys.REVIEW_ANONYMOUS_INDEX, start, end);
        return fetchByIds(ids);
    }

    public long countAnonymous() {
        Long s = redisTemplate.opsForZSet().size(RedisKeys.REVIEW_ANONYMOUS_INDEX);
        return s != null ? s : 0;
    }

    public long count() {
        Long size = redisTemplate.opsForZSet().size(RedisKeys.REVIEW_INDEX);
        return size != null ? size : 0;
    }

    public long countByRepo(String fullName) {
        String[] parts = fullName.split("/", 2);
        Long size = redisTemplate.opsForZSet().size(RedisKeys.reviewRepoKey(parts[0], parts[1]));
        return size != null ? size : 0;
    }

    public List<ReviewSession> findAllReviews() {
        Set<Object> ids = redisTemplate.opsForZSet().reverseRange(RedisKeys.REVIEW_INDEX, 0, -1);
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
        if (session != null) {
            if (session.getRepositoryFullName() != null) {
                String[] parts = session.getRepositoryFullName().split("/", 2);
                redisTemplate.opsForZSet().remove(RedisKeys.reviewRepoKey(parts[0], parts[1]), id);
            }
            if (session.getUserId() != null) {
                redisTemplate.opsForZSet().remove(RedisKeys.reviewUserKey(session.getUserId()), id);
            } else {
                redisTemplate.opsForZSet().remove(RedisKeys.REVIEW_ANONYMOUS_INDEX, id);
            }
        }
        redisTemplate.opsForZSet().remove(RedisKeys.REVIEW_INDEX, id);
        redisTemplate.delete(RedisKeys.reviewKey(id));
    }

    public boolean exists(String id) {
        return redisTemplate.hasKey(RedisKeys.reviewKey(id));
    }

    public boolean isAutoPublishEnabled(String owner, String repo) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.autoPublishKey(owner, repo)));
    }

    public void setAutoPublishEnabled(String owner, String repo, boolean enabled) {
        if (enabled) {
            redisTemplate.opsForValue().set(RedisKeys.autoPublishKey(owner, repo), "1");
        } else {
            redisTemplate.delete(RedisKeys.autoPublishKey(owner, repo));
        }
    }
}

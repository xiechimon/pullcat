package com.pullcat.service.analysis;

import com.pullcat.model.Repo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
public class RepoRepository {

    private static final String KEY_PREFIX = "repo:";
    private static final String INDEX_KEY = "repo:index";

    private final RedisTemplate<String, Object> redisTemplate;

    public RepoRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Repo repo) {
        String key = KEY_PREFIX + repo.getFullName();
        redisTemplate.opsForValue().set(key, repo);
        redisTemplate.opsForSet().add(INDEX_KEY, repo.getFullName());
    }

    public Repo findById(String fullName) {
        Object obj = redisTemplate.opsForValue().get(KEY_PREFIX + fullName);
        return obj instanceof Repo ? (Repo) obj : null;
    }

    public List<Repo> findAll() {
        Set<Object> members = redisTemplate.opsForSet().members(INDEX_KEY);
        List<Repo> repos = new ArrayList<>();
        if (members != null) {
            for (Object member : members) {
                String fullName = member.toString();
                Repo repo = findById(fullName);
                if (repo != null) {
                    repos.add(repo);
                }
            }
        }
        return repos;
    }

    public void delete(String owner, String repo) {
        String fullName = owner + "/" + repo;
        redisTemplate.delete(KEY_PREFIX + fullName);
        redisTemplate.opsForSet().remove(INDEX_KEY, fullName);
    }

    public boolean exists(String owner, String repo) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + owner + "/" + repo));
    }
}

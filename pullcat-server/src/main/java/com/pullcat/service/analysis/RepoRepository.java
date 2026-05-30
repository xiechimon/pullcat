package com.pullcat.service.analysis;

import com.pullcat.config.RedisKeys;
import com.pullcat.model.Repo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
public class RepoRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    public RepoRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Repo repo) {
        redisTemplate.opsForValue().set(RedisKeys.repoKey(repo.getFullName()), repo);
        redisTemplate.opsForSet().add(RedisKeys.REPO_INDEX, repo.getFullName());
    }

    public Repo findById(String fullName) {
        Object obj = redisTemplate.opsForValue().get(RedisKeys.repoKey(fullName));
        return obj instanceof Repo ? (Repo) obj : null;
    }

    public List<Repo> findAll() {
        Set<Object> members = redisTemplate.opsForSet().members(RedisKeys.REPO_INDEX);
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
        redisTemplate.delete(RedisKeys.repoKey(fullName));
        redisTemplate.opsForSet().remove(RedisKeys.REPO_INDEX, fullName);
    }

    public boolean exists(String owner, String repo) {
        return redisTemplate.hasKey(RedisKeys.repoKey(owner + "/" + repo));
    }
}

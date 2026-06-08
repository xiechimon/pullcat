package com.pullcat.service.analysis;

import com.pullcat.dao.entity.RuleDO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class RuleRepository {

    private static final String KEY_PREFIX = "rule:";

    private final RedisTemplate<String, Object> redisTemplate;

    public RuleRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String baseKey(String owner, String repo) {
        return KEY_PREFIX + owner + "/" + repo;
    }

    public void save(RuleDO rule) {
        redisTemplate.opsForHash().put(baseKey(rule.getRepoOwner(), rule.getRepoName()), rule.getId(), rule);
    }

    public List<RuleDO> findByRepo(String owner, String repo) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(baseKey(owner, repo));
        List<RuleDO> rules = new ArrayList<>();
        for (Object obj : entries.values()) {
            if (obj instanceof RuleDO) rules.add((RuleDO) obj);
        }
        return rules;
    }

    public Optional<RuleDO> findById(String owner, String repo, String ruleId) {
        Object obj = redisTemplate.opsForHash().get(baseKey(owner, repo), ruleId);
        return obj instanceof RuleDO ? Optional.of((RuleDO) obj) : Optional.empty();
    }

    public void delete(String owner, String repo, String ruleId) {
        redisTemplate.opsForHash().delete(baseKey(owner, repo), ruleId);
    }
}

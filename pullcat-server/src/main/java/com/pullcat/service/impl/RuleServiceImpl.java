package com.pullcat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.constant.RedisKeys;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.dao.entity.RuleDO;
import com.pullcat.dao.mapper.RuleMapper;
import com.pullcat.dto.req.RuleUpsertReqDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.RuleRespDTO;
import com.pullcat.service.RuleService;
import com.pullcat.service.analysis.RuleSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final RuleMapper ruleMapper;
    private final RuleSuggestionService ruleSuggestionService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<RuleRespDTO> list(String owner, String repo) {
        return findRules(owner, repo).stream().map(this::toRespDTO).toList();
    }

    @Override
    public List<RuleRespDTO> getSuggestions(String owner, String repo) {
        return ruleSuggestionService.getSuggestions(owner, repo).stream().map(this::toRespDTO).toList();
    }

    @Override
    public RuleRespDTO create(String owner, String repo, RuleUpsertReqDTO req) {
        RuleDO rule = toDO(req);
        rule.setId(UUID.randomUUID().toString());
        rule.setRepoOwner(owner);
        rule.setRepoName(repo);
        ruleMapper.insert(rule);
        evictRuleCache(owner, repo);
        return toRespDTO(rule);
    }

    @Override
    public RuleRespDTO update(String owner, String repo, String ruleId, RuleUpsertReqDTO req) {
        RuleDO rule = toDO(req);
        rule.setId(ruleId);
        rule.setRepoOwner(owner);
        rule.setRepoName(repo);
        ruleMapper.updateById(rule);
        evictRuleCache(owner, repo);
        return toRespDTO(rule);
    }

    @Override
    public DeletedRespDTO delete(String owner, String repo, String ruleId) {
        ruleMapper.deleteById(ruleId);
        evictRuleCache(owner, repo);
        return new DeletedRespDTO(true);
    }

    @Override
    public RuleRespDTO toggle(String owner, String repo, String ruleId) {
        RuleDO rule = ruleMapper.selectOne(new LambdaQueryWrapper<RuleDO>()
                .eq(RuleDO::getId, ruleId)
                .eq(RuleDO::getRepoOwner, owner)
                .eq(RuleDO::getRepoName, repo));
        if (rule == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "规则不存在");
        }
        rule.setEnabled(!rule.isEnabled());
        ruleMapper.updateById(rule);
        evictRuleCache(owner, repo);
        return toRespDTO(rule);
    }

    public List<RuleDO> findRules(String owner, String repo) {
        String cacheKey = RedisKeys.ruleKey(owner, repo);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<RuleDO> cachedRules = (List<RuleDO>) cached;
            return cachedRules;
        }

        List<RuleDO> rules = ruleMapper.selectList(new LambdaQueryWrapper<RuleDO>()
                .eq(RuleDO::getRepoOwner, owner)
                .eq(RuleDO::getRepoName, repo));
        redisTemplate.opsForValue().set(cacheKey, rules);
        return rules;
    }

    private void evictRuleCache(String owner, String repo) {
        redisTemplate.delete(RedisKeys.ruleKey(owner, repo));
    }

    private RuleDO toDO(RuleUpsertReqDTO req) {
        RuleDO rule = new RuleDO();
        rule.setType(req.getType());
        rule.setPattern(req.getPattern());
        rule.setSeverity(req.getSeverity());
        rule.setName(req.getName());
        rule.setMessage(req.getMessage());
        rule.setSuggestion(req.getSuggestion());
        rule.setEnabled(req.isEnabled());
        return rule;
    }

    private RuleRespDTO toRespDTO(RuleDO rule) {
        RuleRespDTO resp = new RuleRespDTO();
        resp.setId(rule.getId());
        resp.setName(rule.getName());
        resp.setType(rule.getType());
        resp.setPattern(rule.getPattern());
        resp.setSeverity(rule.getSeverity());
        resp.setMessage(rule.getMessage());
        resp.setSuggestion(rule.getSuggestion());
        resp.setEnabled(rule.isEnabled());
        return resp;
    }
}

package com.pullcat.service.analysis.impl;

import com.pullcat.common.enums.RuleType;
import com.pullcat.common.enums.Severity;
import com.pullcat.dao.mapper.RuleMapper;
import com.pullcat.dao.entity.RuleDO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.dto.resp.ReviewSessionRespDTO;
import com.pullcat.service.analysis.ReviewSessionService;
import com.pullcat.service.analysis.RuleSuggestionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RuleSuggestionServiceImpl implements RuleSuggestionService {

    private static final String CACHE_PREFIX = "rule-suggestions:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final ReviewSessionService reviewSessionService;
    private final RuleMapper ruleMapper;
    private final ChatClient lightChatClient;
    private final RedisTemplate<String, Object> redisTemplate;

    public RuleSuggestionServiceImpl(ReviewSessionService reviewSessionService,
                                     RuleMapper ruleMapper,
                                     @Qualifier("lightChatClient") ChatClient lightChatClient,
                                     RedisTemplate<String, Object> redisTemplate) {
        this.reviewSessionService = reviewSessionService;
        this.ruleMapper = ruleMapper;
        this.lightChatClient = lightChatClient;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<RuleDO> suggestRules(String owner, String repo) {
        String cacheKey = CACHE_PREFIX + owner + "/" + repo;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) {
            @SuppressWarnings("unchecked")
            List<RuleDO> cachedList = (List<RuleDO>) cached;
            return cachedList;
        }

        List<ReviewSessionRespDTO> sessions = reviewSessionService.findByRepo(owner + "/" + repo, 0, 10);
        if (sessions.size() < 2) {
            return Collections.emptyList();
        }

        Map<String, FrequencyEntry> freqMap = new LinkedHashMap<>();
        for (ReviewSessionRespDTO session : sessions) {
            for (var result : session.getAnalyses().values()) {
                if (result.getIssues() == null) {
                    continue;
                }
                for (IssueRespDTO issue : result.getIssues()) {
                    String key = issue.getTitle() != null ? issue.getTitle().trim() : "";
                    if (key.isEmpty()) {
                        continue;
                    }
                    freqMap.computeIfAbsent(key, FrequencyEntry::new).increment(issue);
                }
            }
        }

        List<FrequencyEntry> frequentIssues = freqMap.values().stream()
                .filter(entry -> entry.count >= 2)
                .sorted((a, b) -> Integer.compare(b.count, a.count))
                .limit(5)
                .toList();

        if (frequentIssues.isEmpty()) {
            cacheEmpty(cacheKey);
            return Collections.emptyList();
        }

        String prompt = buildSuggestionPrompt(frequentIssues);

        try {
            String response = lightChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            List<RuleDO> suggestions = parseSuggestions(response);
            redisTemplate.opsForValue().set(cacheKey, suggestions, CACHE_TTL);
            return suggestions;
        } catch (Exception e) {
            log.warn("Failed to generate rule suggestions for {}/{}: {}", owner, repo, e.getMessage());
            cacheEmpty(cacheKey);
            return Collections.emptyList();
        }
    }

    @Override
    public List<RuleDO> getSuggestions(String owner, String repo) {
        List<RuleDO> aiRules = suggestRules(owner, repo);

        List<RuleDO> existingRules = ruleMapper.selectList(new LambdaQueryWrapper<RuleDO>()
                                                                   .eq(RuleDO::getRepoOwner, owner)
                                                                   .eq(RuleDO::getRepoName, repo));
        Set<String> existingNames = existingRules.stream()
                .map(RuleDO::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return aiRules.stream()
                .filter(rule -> rule.getName() == null || !existingNames.contains(rule.getName()))
                .toList();
    }

    @Override
    public boolean hasNewSuggestions(String owner, String repo) {
        return !getSuggestions(owner, repo).isEmpty();
    }

    private String buildSuggestionPrompt(List<FrequencyEntry> frequentIssues) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是该仓库历史代码审查中反复出现的问题。请为每个问题生成一条正则规则来检测它。\n\n");

        for (int i = 0; i < frequentIssues.size(); i++) {
            FrequencyEntry entry = frequentIssues.get(i);
            sb.append("问题 ").append(i + 1).append(": ").append(entry.issueTitle).append("\n");
            sb.append("出现次数: ").append(entry.count).append("\n");
            sb.append("描述: ").append(entry.sampleIssue.getDescription()).append("\n");
            sb.append("修改建议: ").append(entry.sampleIssue.getSuggestion()).append("\n\n");
        }

        sb.append("请为每个问题生成规则，规则类型使用 CODE_PATTERN（代码模式匹配）。");
        sb.append("pattern 字段请使用 Java 正则表达式，匹配该问题对应的代码模式。\n");
        sb.append("只输出纯 JSON 数组，不包含 markdown 代码块标记：\n");
        sb.append("[{\"type\":\"CODE_PATTERN\",\"pattern\":\"正则表达式\",\"severity\":\"HIGH\",\"name\":\"规则名称\",\"message\":\"问题描述\",\"suggestion\":\"修复建议\"}]");
        return sb.toString();
    }

    private List<RuleDO> parseSuggestions(String response) {
        try {
            String json = com.pullcat.toolkit.JsonOutputParser.extractJson(response);
            if (!json.trim().startsWith("[")) {
                json = "[" + json + "]";
            }

            var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                    .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
                    .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            var nodes = mapper.readTree(json);
            List<RuleDO> rules = new ArrayList<>();
            for (var node : nodes) {
                RuleDO rule = new RuleDO();
                rule.setId(UUID.randomUUID().toString());
                rule.setType(RuleType.CODE_PATTERN);
                String typeStr = node.has("type") ? node.get("type").asText() : "CODE_PATTERN";
                try {
                    rule.setType(RuleType.valueOf(typeStr));
                } catch (IllegalArgumentException ignored) {
                }
                rule.setPattern(node.has("pattern") ? node.get("pattern").asText() : "");
                rule.setName(node.has("name") ? node.get("name").asText() : "");
                rule.setMessage(node.has("message") ? node.get("message").asText() : "");
                rule.setSuggestion(node.has("suggestion") ? node.get("suggestion").asText() : "");
                String severityStr = node.has("severity") ? node.get("severity").asText() : "MEDIUM";
                try {
                    rule.setSeverity(Severity.valueOf(severityStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    rule.setSeverity(Severity.MEDIUM);
                }
                rule.setEnabled(true);
                rules.add(rule);
            }
            return rules;
        } catch (Exception e) {
            log.warn("Failed to parse rule suggestions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void cacheEmpty(String cacheKey) {
        redisTemplate.opsForValue().set(cacheKey, Collections.emptyList(), Duration.ofMinutes(10));
    }

    private static class FrequencyEntry {
        final String issueTitle;
        int count;
        IssueRespDTO sampleIssue;

        private FrequencyEntry(String title) {
            this.issueTitle = title;
        }

        private void increment(IssueRespDTO issue) {
            count++;
            if (sampleIssue == null || issue.getDescription() != null) {
                sampleIssue = issue;
            }
        }
    }
}

package com.pullcat.service.analysis;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.dao.entity.RuleDO;
import com.pullcat.dto.req.RuleUpsertReqDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.RuleRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 规则业务服务
 */
@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;
    private final RuleSuggestionService ruleSuggestionService;

    /**
     * 查询仓库所有规则
     */
    public List<RuleRespDTO> list(String owner, String repo) {
        return ruleRepository.findByRepo(owner, repo).stream().map(this::toRespDTO).toList();
    }

    /**
     * 查询仓库规则建议
     */
    public List<RuleRespDTO> getSuggestions(String owner, String repo) {
        return ruleSuggestionService.getSuggestions(owner, repo).stream().map(this::toRespDTO).toList();
    }

    /**
     * 创建规则
     */
    public RuleRespDTO create(String owner, String repo, RuleUpsertReqDTO req) {
        RuleDO rule = toDO(req);
        rule.setId(UUID.randomUUID().toString());
        rule.setRepoOwner(owner);
        rule.setRepoName(repo);
        ruleRepository.save(rule);
        return toRespDTO(rule);
    }

    /**
     * 更新规则
     */
    public RuleRespDTO update(String owner, String repo, String ruleId, RuleUpsertReqDTO req) {
        RuleDO rule = toDO(req);
        rule.setId(ruleId);
        rule.setRepoOwner(owner);
        rule.setRepoName(repo);
        ruleRepository.save(rule);
        return toRespDTO(rule);
    }

    /**
     * 删除规则
     */
    public DeletedRespDTO delete(String owner, String repo, String ruleId) {
        ruleRepository.delete(owner, repo, ruleId);
        return new DeletedRespDTO(true);
    }

    /**
     * 切换规则启用状态
     */
    public RuleRespDTO toggle(String owner, String repo, String ruleId) {
        RuleDO rule = ruleRepository.findById(owner, repo, ruleId)
                .orElseThrow(() -> new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "规则不存在"));
        rule.setEnabled(!rule.isEnabled());
        ruleRepository.save(rule);
        return toRespDTO(rule);
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

package com.pullcat.service;

import com.pullcat.dto.req.RuleUpsertReqDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.RuleRespDTO;

import java.util.List;

/**
 * 规则业务服务
 */
public interface RuleService {

    /**
     * 查询仓库所有规则
     */
    List<RuleRespDTO> list(String owner, String repo);

    /**
     * 查询仓库规则建议
     */
    List<RuleRespDTO> getSuggestions(String owner, String repo);

    /**
     * 创建规则
     */
    RuleRespDTO create(String owner, String repo, RuleUpsertReqDTO req);

    /**
     * 更新规则
     */
    RuleRespDTO update(String owner, String repo, String ruleId, RuleUpsertReqDTO req);

    /**
     * 删除规则
     */
    DeletedRespDTO delete(String owner, String repo, String ruleId);

    /**
     * 切换规则启用状态
     */
    RuleRespDTO toggle(String owner, String repo, String ruleId);
}

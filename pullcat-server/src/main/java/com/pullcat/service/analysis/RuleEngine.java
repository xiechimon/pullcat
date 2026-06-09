package com.pullcat.service.analysis;

import com.pullcat.dao.entity.RuleDO;
import com.pullcat.dto.resp.FileContentRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;

import java.util.List;

public interface RuleEngine {

    List<IssueRespDTO> evaluate(List<FileContentRespDTO> files, List<RuleDO> rules);
}

package com.pullcat.service.analysis.impl;

import com.pullcat.common.enums.Severity;
import com.pullcat.dao.entity.RuleDO;
import com.pullcat.dto.resp.FileContentRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.service.analysis.RuleEngine;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class RuleEngineImpl implements RuleEngine {

    @Override
    public List<IssueRespDTO> evaluate(List<FileContentRespDTO> files, List<RuleDO> rules) {
        List<IssueRespDTO> issues = new ArrayList<>();

        for (RuleDO rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }
            Pattern pattern = Pattern.compile(rule.getPattern());

            for (FileContentRespDTO file : files) {
                if (file.isExcluded() || file.getContent() == null) {
                    continue;
                }

                String target = switch (rule.getType()) {
                    case FILE_PATH_MATCH -> file.getPath();
                    case CODE_PATTERN, FORBIDDEN_API -> file.getContent();
                };

                int lineNum = 0;
                for (String line : target.split("\n")) {
                    lineNum++;
                    if (pattern.matcher(line).find()) {
                        IssueRespDTO issue = new IssueRespDTO();
                        issue.setId("RULE-" + UUID.randomUUID().toString().substring(0, 8));
                        issue.setSeverity(rule.getSeverity() != null ? rule.getSeverity() : Severity.MEDIUM);
                        issue.setFile(file.getPath());
                        issue.setLine(lineNum);
                        issue.setTitle(rule.getName());
                        issue.setDescription(rule.getMessage());
                        issue.setSuggestion(rule.getSuggestion());
                        issue.setConfidence(1.0);
                        issue.setSourceDimensions(List.of("RULE_ENGINE"));
                        issues.add(issue);
                    }
                }
            }
        }
        return issues;
    }
}

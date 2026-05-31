package com.pullcat.service.analysis;

import com.pullcat.model.FileContent;
import com.pullcat.model.Issue;
import com.pullcat.model.Rule;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class RuleEngine {

    public List<Issue> evaluate(List<FileContent> files, List<Rule> rules) {
        List<Issue> issues = new ArrayList<>();

        for (Rule rule : rules) {
            if (!rule.isEnabled()) continue;
            Pattern pattern = Pattern.compile(rule.getPattern());

            for (FileContent file : files) {
                if (file.isExcluded() || file.getContent() == null) continue;

                String target = switch (rule.getType()) {
                    case FILE_PATH_MATCH -> file.getPath();
                    case CODE_PATTERN, FORBIDDEN_API -> file.getContent();
                };

                int lineNum = 0;
                for (String line : target.split("\n")) {
                    lineNum++;
                    if (pattern.matcher(line).find()) {
                        Issue issue = new Issue();
                        issue.setId("RULE-" + UUID.randomUUID().toString().substring(0, 8));
                        issue.setSeverity(rule.getSeverity() != null ? rule.getSeverity() : Issue.Severity.MEDIUM);
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

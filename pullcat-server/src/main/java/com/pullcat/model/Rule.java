package com.pullcat.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class Rule {

    private String id;
    private String repoOwner;
    private String repoName;
    private RuleType type;
    private String pattern;
    private Issue.Severity severity;
    private String name;
    private String message;
    private String suggestion;
    private boolean enabled = true;
    private Instant createdAt = Instant.now();

    public enum RuleType {
        FILE_PATH_MATCH, CODE_PATTERN, FORBIDDEN_API
    }
}

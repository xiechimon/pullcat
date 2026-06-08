package com.pullcat.dao.entity;

import com.pullcat.common.enums.RuleType;
import com.pullcat.common.enums.Severity;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class RuleDO {

    private String id;
    private String repoOwner;
    private String repoName;
    private RuleType type;
    private String pattern;
    private Severity severity;
    private String name;
    private String message;
    private String suggestion;
    private boolean enabled = true;
    private Instant createdAt = Instant.now();
}

package com.pullcat.service.analysis;

import com.pullcat.common.enums.RuleType;
import com.pullcat.common.enums.Severity;
import com.pullcat.dto.resp.FileContentRespDTO;
import com.pullcat.dto.resp.IssueRespDTO;
import com.pullcat.dao.entity.RuleDO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    private final RuleEngine ruleEngine = new RuleEngine();

    @Test
    void codePatternRuleMatchesContent() {
        RuleDO rule = createRule("missing-null-check", RuleType.CODE_PATTERN,
                "user\\.getName\\(\\)", Severity.HIGH,
                "Missing null check", "Add null check before calling getName()");

        FileContentRespDTO file = new FileContentRespDTO("src/UserService.java",
                "public String getDisplay() {\n    return user.getName();\n}", "");

        List<IssueRespDTO> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).hasSize(1);
        IssueRespDTO issue = issues.get(0);
        assertThat(issue.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(issue.getFile()).isEqualTo("src/UserService.java");
        assertThat(issue.getLine()).isEqualTo(2);
        assertThat(issue.getTitle()).isEqualTo("missing-null-check");
        assertThat(issue.getConfidence()).isEqualTo(1.0);
        assertThat(issue.getSourceDimensions()).contains("RULE_ENGINE");
    }

    @Test
    void codePatternRuleNoMatch() {
        RuleDO rule = createRule("sql-injection", RuleType.CODE_PATTERN,
                "Statement\\.executeQuery", Severity.CRITICAL,
                "SQL injection", "Use PreparedStatement");

        FileContentRespDTO file = new FileContentRespDTO("src/UserService.java",
                "ps.executeQuery();\nreturn result;", "");

        List<IssueRespDTO> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).isEmpty();
    }

    @Test
    void filePathMatchRule() {
        RuleDO rule = createRule("no-test-files", RuleType.FILE_PATH_MATCH,
                "src/test/", Severity.LOW,
                "Test directory change", "Verify test changes");

        FileContentRespDTO file = new FileContentRespDTO("src/test/UserServiceTest.java",
                "class UserServiceTest {}", "");

        List<IssueRespDTO> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getFile()).isEqualTo("src/test/UserServiceTest.java");
    }

    @Test
    void forbiddenApiRule() {
        RuleDO rule = createRule("no-system-exit", RuleType.FORBIDDEN_API,
                "System\\.exit", Severity.CRITICAL,
                "System.exit call", "Use exception handling instead");

        FileContentRespDTO file = new FileContentRespDTO("src/App.java",
                "if (error) {\n    System.exit(1);\n}", "");

        List<IssueRespDTO> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void disabledRuleIsSkipped() {
        RuleDO rule = createRule("check", RuleType.CODE_PATTERN,
                ".*", Severity.HIGH, "Check", "Fix");
        rule.setEnabled(false);

        FileContentRespDTO file = new FileContentRespDTO("src/App.java", "anything", "");

        List<IssueRespDTO> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).isEmpty();
    }

    @Test
    void excludedFileIsSkipped() {
        RuleDO rule = createRule("check", RuleType.CODE_PATTERN,
                ".*", Severity.HIGH, "Check", "Fix");

        FileContentRespDTO file = new FileContentRespDTO("src/App.java", "anything", "");
        file.setExcluded(true);

        List<IssueRespDTO> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).isEmpty();
    }

    @Test
    void multipleMatchesInSameFile() {
        RuleDO rule = createRule("println", RuleType.CODE_PATTERN,
                "System\\.out\\.println", Severity.LOW,
                "Avoid println", "Use logger instead");

        FileContentRespDTO file = new FileContentRespDTO("src/Debug.java",
                "System.out.println(\"a\");\nint x = 1;\nSystem.out.println(\"b\");", "");

        List<IssueRespDTO> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).hasSize(2);
        assertThat(issues.get(0).getLine()).isEqualTo(1);
        assertThat(issues.get(1).getLine()).isEqualTo(3);
    }

    @Test
    void nullFileContentIsSkipped() {
        RuleDO rule = createRule("check", RuleType.CODE_PATTERN,
                ".*", Severity.HIGH, "Check", "Fix");

        FileContentRespDTO file = new FileContentRespDTO("src/App.java", null, "");

        List<IssueRespDTO> issues = ruleEngine.evaluate(List.of(file), List.of(rule));

        assertThat(issues).isEmpty();
    }

    @Test
    void multipleRulesAgainstSameFile() {
        RuleDO rule1 = createRule("rule1", RuleType.CODE_PATTERN,
                "TODO", Severity.LOW, "TODO found", "Remove TODO");
        RuleDO rule2 = createRule("rule2", RuleType.CODE_PATTERN,
                "FIXME", Severity.MEDIUM, "FIXME found", "Fix the issue");

        FileContentRespDTO file = new FileContentRespDTO("src/App.java",
                "// TODO implement\n// FIXME bug here", "");

        List<IssueRespDTO> issues = ruleEngine.evaluate(List.of(file), List.of(rule1, rule2));

        assertThat(issues).hasSize(2);
    }

    private static RuleDO createRule(String name, RuleType type, String pattern,
                                   Severity severity, String message, String suggestion) {
        RuleDO rule = new RuleDO();
        rule.setName(name);
        rule.setType(type);
        rule.setPattern(pattern);
        rule.setSeverity(severity);
        rule.setMessage(message);
        rule.setSuggestion(suggestion);
        rule.setEnabled(true);
        return rule;
    }
}

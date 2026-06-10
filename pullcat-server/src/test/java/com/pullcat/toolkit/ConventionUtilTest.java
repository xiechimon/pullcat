package com.pullcat.toolkit;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ConventionUtilTest {

    @Test
    void returnsEmptyForNullOrBlankTree() {
        assertThat(ConventionUtil.detectConventionCandidates(null)).isEmpty();
        assertThat(ConventionUtil.detectConventionCandidates("")).isEmpty();
        assertThat(ConventionUtil.detectConventionCandidates("   ")).isEmpty();
    }

    @Test
    void excludesKnownNonConventionFiles() {
        String tree = "./\n  README.md\n  CHANGELOG.md\n  LICENSE.md\n  AGENTS.md\n\nsrc/\n  Main.java\n";
        List<String> result = ConventionUtil.detectConventionCandidates(tree);
        assertThat(result).containsExactly("AGENTS.md");
    }

    @Test
    void returnsAllWhenFewerThanThree() {
        String tree = "./\n  AGENTS.md\n  CONTRIBUTING.md\n\n";
        List<String> result = ConventionUtil.detectConventionCandidates(tree);
        assertThat(result).containsExactly("AGENTS.md", "CONTRIBUTING.md");
    }

    @Test
    void sortsByPriorityThenLimitsToThree() {
        String tree = "./\n  CONTRIBUTING.md\n  CLAUDE.md\n  AGENTS.md\n  DEVELOPMENT.md\n  CONVENTIONS.md\n\n";
        List<String> result = ConventionUtil.detectConventionCandidates(tree);
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo("AGENTS.md");
        assertThat(result.get(1)).isEqualTo("CLAUDE.md");
        assertThat(result.get(2)).isEqualTo("CONTRIBUTING.md");
    }

    @Test
    void caseInsensitiveExclusion() {
        String tree = "./\n  readme.md\n  changelog.MD\n  AGENTS.md\n\n";
        List<String> result = ConventionUtil.detectConventionCandidates(tree);
        assertThat(result).containsExactly("AGENTS.md");
    }
}

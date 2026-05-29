package com.pullcat.service.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptLoaderTest {

    private final PromptLoader loader = new PromptLoader(
            new PathMatchingResourcePatternResolver(new DefaultResourceLoader()));

    @Test
    void loadTemplateSuccess() {
        String template = loader.loadTemplate("summary");

        assertThat(template).contains("审查专家");
        assertThat(template).contains("{pr_info}");
    }

    @Test
    void loadTemplateNotFound() {
        assertThatThrownBy(() -> loader.loadTemplate("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Prompt template not found");
    }

    @Test
    void populateTemplate() {
        String template = "Hello {name}, welcome to {place}!";
        Map<String, String> vars = Map.of("name", "World", "place", "pullcat");

        String result = loader.populateTemplate(template, vars);

        assertThat(result).isEqualTo("Hello World, welcome to pullcat!");
    }

    @Test
    void populateTemplateNullValue() {
        String template = "Key is {key}.";
        java.util.Map<String, String> vars = new java.util.HashMap<>();
        vars.put("key", null);

        String result = loader.populateTemplate(template, vars);

        assertThat(result).isEqualTo("Key is .");
    }

    @Test
    void loadAndPopulate() {
        String result = loader.loadAndPopulate("summary", Map.of("pr_info", "TEST", "file_tree", "", "changed_files", ""));

        assertThat(result).contains("TEST");
        assertThat(result).doesNotContain("{pr_info}");
    }

    @Test
    void allTemplatesLoadable() {
        for (String name : new String[]{"summary", "risk", "quality", "consistency", "testing"}) {
            String template = loader.loadTemplate(name);
            assertThat(template).isNotEmpty();
        }
    }
}

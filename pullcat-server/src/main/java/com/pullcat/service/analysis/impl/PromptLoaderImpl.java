package com.pullcat.service.analysis.impl;

import com.pullcat.service.analysis.PromptLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Prompt 模板加载器，从 classpath 读取模板文件并执行变量替换
 */
@Slf4j
@Component
public class PromptLoaderImpl implements PromptLoader {

    private final ResourcePatternResolver resolver;

    public PromptLoaderImpl(ResourcePatternResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public String loadTemplate(String templateName) {
        try {
            Resource resource = resolver.getResource("classpath:prompts/" + templateName + ".md");
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load prompt template: {}", templateName, e);
            throw new RuntimeException("Prompt template not found: " + templateName, e);
        }
    }

    @Override
    public String populateTemplate(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    @Override
    public String loadAndPopulate(String templateName, Map<String, String> variables) {
        String template = loadTemplate(templateName);
        return populateTemplate(template, variables);
    }
}

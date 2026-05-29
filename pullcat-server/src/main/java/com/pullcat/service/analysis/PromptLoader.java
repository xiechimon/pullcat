package com.pullcat.service.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Prompt 模板加载器，从 classpath 读取模板文件并执行变量替换。
 */
@Slf4j
@Component
public class PromptLoader {

    private final ResourcePatternResolver resolver;

    public PromptLoader(ResourcePatternResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 从 classpath 加载指定名称的 Prompt 模板。
     *
     * @param templateName 模板名称（不含扩展名），对应 prompts/ 目录下的 .md 文件
     * @return 模板原文
     */
    public String loadTemplate(String templateName) {
        try {
            Resource resource = resolver.getResource("classpath:prompts/" + templateName + ".md");
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load prompt template: {}", templateName, e);
            throw new RuntimeException("Prompt template not found: " + templateName, e);
        }
    }

    /**
     * 将模板中的 {variable} 占位符替换为实际值。
     *
     * @param template  包含 {variable} 占位符的模板字符串
     * @param variables 变量名到值的映射
     * @return 替换后的字符串
     */
    public String populateTemplate(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    /**
     * 加载模板并执行变量替换，一步完成。
     */
    public String loadAndPopulate(String templateName, Map<String, String> variables) {
        String template = loadTemplate(templateName);
        return populateTemplate(template, variables);
    }
}

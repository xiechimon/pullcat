package com.pullcat.service.analysis;

import java.util.Map;

public interface PromptLoader {

    String loadTemplate(String templateName);

    String populateTemplate(String template, Map<String, String> variables);

    String loadAndPopulate(String templateName, Map<String, String> variables);
}

package com.pullcat.service.analysis;

import com.pullcat.dao.entity.RuleDO;

import java.util.List;

public interface RuleSuggestionService {

    List<RuleDO> suggestRules(String owner, String repo);

    List<RuleDO> getSuggestions(String owner, String repo);

    boolean hasNewSuggestions(String owner, String repo);
}

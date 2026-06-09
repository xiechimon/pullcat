package com.pullcat.service.llm;

import com.pullcat.common.enums.AnalysisType;
import com.pullcat.dto.resp.AnalysisResultRespDTO;
import reactor.core.publisher.Mono;

public interface AnalysisService {

    Mono<AnalysisResultRespDTO> execute(String prompt);

    String getTemplateName();

    AnalysisType getType();

    AnalysisResultRespDTO getResult();
}

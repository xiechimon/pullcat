package com.pullcat.controller;

import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.dto.req.CompareReviewsReqDTO;
import com.pullcat.dto.resp.CompareReviewsRespDTO;
import com.pullcat.service.CompareService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CompareController {

    private final CompareService compareService;

    public CompareController(CompareService compareService) {
        this.compareService = compareService;
    }

    @PostMapping("/reviews/compare")
    public Result<CompareReviewsRespDTO> compare(@RequestBody CompareReviewsReqDTO requestParam) {
        var ids = requestParam.getReviewIds();
        if (ids == null || ids.size() != 2) {
            throw new ClientException(CommonErrorCodeEnum.CLIENT_ERROR.code(), "必须提供 2 个 reviewIds");
        }
        return Results.success(compareService.compare(ids.get(0), ids.get(1)));
    }
}

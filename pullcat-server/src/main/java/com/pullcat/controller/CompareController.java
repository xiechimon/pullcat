package com.pullcat.controller;

import com.pullcat.common.convention.result.Result;
import com.pullcat.common.convention.result.Results;
import com.pullcat.dto.req.CompareReviewsReqDTO;
import com.pullcat.dto.resp.CompareReviewsRespDTO;
import com.pullcat.service.CompareService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pullcat/v1")
public class CompareController {

    private final CompareService compareService;

    public CompareController(CompareService compareService) {
        this.compareService = compareService;
    }

    @PostMapping("/reviews/compare")
    public Result<CompareReviewsRespDTO> compare(@RequestBody CompareReviewsReqDTO requestParam) {
        return Results.success(compareService.compare(requestParam.getReviewIds()));
    }
}

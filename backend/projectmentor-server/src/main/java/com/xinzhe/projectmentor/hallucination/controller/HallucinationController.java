package com.xinzhe.projectmentor.hallucination.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.hallucination.dto.HallucinationCheckRequest;
import com.xinzhe.projectmentor.hallucination.service.HallucinationCheckService;
import com.xinzhe.projectmentor.hallucination.vo.HallucinationCheckResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hallucination")
@RequiredArgsConstructor
public class HallucinationController {

    private final HallucinationCheckService hallucinationCheckService;

    @PostMapping("/check")
    public Result<HallucinationCheckResultVO> check(@Valid @RequestBody HallucinationCheckRequest request) {
        return Result.success(hallucinationCheckService.check(request));
    }
}
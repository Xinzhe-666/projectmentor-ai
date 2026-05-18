package com.xinzhe.projectmentor.credit.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.credit.dto.AddCreditRequest;
import com.xinzhe.projectmentor.credit.service.CreditService;
import com.xinzhe.projectmentor.credit.vo.CreditInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/credits")
@RequiredArgsConstructor
public class AdminCreditController {

    private final CreditService creditService;

    @PostMapping("/add")
    public Result<CreditInfoVO> addCredits(@Valid @RequestBody AddCreditRequest request) {
        return Result.success(creditService.addCreditsByAdmin(request));
    }
}
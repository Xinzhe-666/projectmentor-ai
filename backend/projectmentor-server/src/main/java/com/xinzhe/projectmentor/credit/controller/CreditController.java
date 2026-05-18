package com.xinzhe.projectmentor.credit.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.credit.service.CreditService;
import com.xinzhe.projectmentor.credit.vo.CreditInfoVO;
import com.xinzhe.projectmentor.credit.vo.CreditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @GetMapping("/me")
    public Result<CreditInfoVO> getMyCredits() {
        return Result.success(creditService.getMyCredits());
    }

    @GetMapping("/logs")
    public Result<List<CreditLogVO>> listMyCreditLogs() {
        return Result.success(creditService.listMyCreditLogs());
    }
}
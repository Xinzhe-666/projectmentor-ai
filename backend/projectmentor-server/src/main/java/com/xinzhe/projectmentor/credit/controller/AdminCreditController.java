package com.xinzhe.projectmentor.credit.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.admin.service.AdminService;
import com.xinzhe.projectmentor.credit.dto.AdminGrantCreditRequest;
import com.xinzhe.projectmentor.credit.dto.AddCreditRequest;
import com.xinzhe.projectmentor.credit.service.CreditService;
import com.xinzhe.projectmentor.credit.vo.AdminCreditGrantResultVO;
import com.xinzhe.projectmentor.credit.vo.AdminCreditUserDetailVO;
import com.xinzhe.projectmentor.credit.vo.AdminCreditUserVO;
import com.xinzhe.projectmentor.credit.vo.CreditInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/credits")
@RequiredArgsConstructor
public class AdminCreditController {

    private final CreditService creditService;

    private final AdminService adminService;

    @GetMapping("/users")
    public Result<List<AdminCreditUserVO>> searchUsers(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Integer limit) {
        return Result.success(creditService.searchCreditUsers(keyword, limit));
    }

    @GetMapping("/users/{userId}")
    public Result<AdminCreditUserDetailVO> getUserDetail(@PathVariable Long userId) {
        return Result.success(creditService.getAdminCreditUserDetail(userId));
    }

    @PostMapping("/grant")
    public Result<AdminCreditGrantResultVO> grantCredits(@Valid @RequestBody AdminGrantCreditRequest request) {
        return Result.success(creditService.grantCreditsByAdmin(request));
    }

    @PostMapping("/add")
    public Result<CreditInfoVO> addCredits(@Valid @RequestBody AddCreditRequest request) {
        adminService.requireAdmin();
        return Result.success(creditService.addCreditsByAdmin(request));
    }
}

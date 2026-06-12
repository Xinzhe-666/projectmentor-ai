package com.xinzhe.projectmentor.credit.controller;

import com.xinzhe.projectmentor.admin.service.AdminService;
import com.xinzhe.projectmentor.common.PageResult;
import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.credit.dto.AdminCreditAdjustmentRequest;
import com.xinzhe.projectmentor.credit.dto.AdminGrantCreditRequest;
import com.xinzhe.projectmentor.credit.dto.AddCreditRequest;
import com.xinzhe.projectmentor.credit.service.CreditService;
import com.xinzhe.projectmentor.credit.vo.AdminCreditAdjustmentResultVO;
import com.xinzhe.projectmentor.credit.vo.AdminCreditGrantResultVO;
import com.xinzhe.projectmentor.credit.vo.AdminCreditUserDetailVO;
import com.xinzhe.projectmentor.credit.vo.AdminCreditUserVO;
import com.xinzhe.projectmentor.credit.vo.AdminCreditTransactionVO;
import com.xinzhe.projectmentor.credit.vo.CreditInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/credits")
@RequiredArgsConstructor
public class AdminCreditController {

    private final CreditService creditService;

    private final AdminService adminService;

    @GetMapping("/users")
    public Result<PageResult<AdminCreditUserVO>> searchUsers(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size,
                                                             @RequestParam(required = false) String sort) {
        return Result.success(creditService.searchCreditUsers(keyword, page, size, sort));
    }

    @GetMapping("/users/{userId}")
    public Result<AdminCreditUserDetailVO> getUserDetail(@PathVariable Long userId) {
        return Result.success(creditService.getAdminCreditUserDetail(userId));
    }

    @GetMapping("/users/{userId}/logs")
    public Result<PageResult<AdminCreditTransactionVO>> getUserLogs(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String module,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(creditService.listAdminCreditLogs(
                userId, page, size, type, module, startTime, endTime
        ));
    }

    @PostMapping("/users/{userId}/grant")
    public Result<AdminCreditAdjustmentResultVO> grantUserCredits(
            @PathVariable Long userId,
            @Valid @RequestBody AdminCreditAdjustmentRequest request) {
        return Result.success(creditService.grantCreditsByAdmin(userId, request));
    }

    @PostMapping("/users/{userId}/deduct")
    public Result<AdminCreditAdjustmentResultVO> deductUserCredits(
            @PathVariable Long userId,
            @Valid @RequestBody AdminCreditAdjustmentRequest request) {
        return Result.success(creditService.deductCreditsByAdmin(userId, request));
    }

    /**
     * Compatibility endpoint retained for older admin clients.
     */
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

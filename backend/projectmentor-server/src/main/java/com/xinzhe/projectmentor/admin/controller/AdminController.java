package com.xinzhe.projectmentor.admin.controller;

import com.xinzhe.projectmentor.admin.service.AdminService;
import com.xinzhe.projectmentor.admin.service.AdminAiUsageService;
import com.xinzhe.projectmentor.admin.vo.AdminAiUsageOverviewVO;
import com.xinzhe.projectmentor.admin.vo.AdminMeVO;
import com.xinzhe.projectmentor.admin.vo.AdminRecentProjectVO;
import com.xinzhe.projectmentor.admin.vo.AdminRecentQaVO;
import com.xinzhe.projectmentor.admin.vo.AdminRecentReportVO;
import com.xinzhe.projectmentor.admin.vo.AdminRecentUserVO;
import com.xinzhe.projectmentor.admin.vo.AdminStatsVO;
import com.xinzhe.projectmentor.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    private final AdminAiUsageService adminAiUsageService;

    @GetMapping("/me")
    public Result<AdminMeVO> me() {
        return Result.success(adminService.getCurrentAdminMe());
    }

    @GetMapping("/stats")
    public Result<AdminStatsVO> stats() {
        return Result.success(adminService.getStats());
    }

    @GetMapping("/recent/users")
    public Result<List<AdminRecentUserVO>> recentUsers(@RequestParam(required = false) Integer limit) {
        return Result.success(adminService.listRecentUsers(limit));
    }

    @GetMapping("/recent/projects")
    public Result<List<AdminRecentProjectVO>> recentProjects(@RequestParam(required = false) Integer limit) {
        return Result.success(adminService.listRecentProjects(limit));
    }

    @GetMapping("/recent/reports")
    public Result<List<AdminRecentReportVO>> recentReports(@RequestParam(required = false) Integer limit) {
        return Result.success(adminService.listRecentReports(limit));
    }

    @GetMapping("/recent/qa")
    public Result<List<AdminRecentQaVO>> recentQa(@RequestParam(required = false) Integer limit) {
        return Result.success(adminService.listRecentQa(limit));
    }

    @GetMapping("/ai-usage/overview")
    public Result<AdminAiUsageOverviewVO> aiUsageOverview() {
        return Result.success(adminAiUsageService.getOverview());
    }
}

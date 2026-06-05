package com.xinzhe.projectmentor.dashboard.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.dashboard.service.DashboardService;
import com.xinzhe.projectmentor.dashboard.vo.DashboardSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public Result<DashboardSummaryVO> getSummary() {
        return Result.success(dashboardService.getSummary());
    }
}

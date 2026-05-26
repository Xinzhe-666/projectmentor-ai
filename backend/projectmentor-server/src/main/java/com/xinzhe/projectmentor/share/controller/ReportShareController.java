package com.xinzhe.projectmentor.share.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.share.service.ReportShareService;
import com.xinzhe.projectmentor.share.vo.PublicReportVO;
import com.xinzhe.projectmentor.share.vo.ReportShareVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportShareController {

    private final ReportShareService reportShareService;

    @GetMapping("/api/reports/{reportId}/share")
    public Result<ReportShareVO> getShareInfo(@PathVariable Long reportId) {
        return Result.success(reportShareService.getShareInfo(reportId));
    }

    @PostMapping("/api/reports/{reportId}/share")
    public Result<ReportShareVO> createOrRefreshShare(@PathVariable Long reportId) {
        return Result.success(reportShareService.createOrRefreshShare(reportId));
    }

    @DeleteMapping("/api/reports/{reportId}/share")
    public Result<Void> disableShare(@PathVariable Long reportId) {
        reportShareService.disableShare(reportId);
        return Result.success();
    }

    @GetMapping("/api/share/reports/{token}")
    public Result<PublicReportVO> getPublicReport(@PathVariable String token) {
        return Result.success(reportShareService.getPublicReport(token));
    }
}

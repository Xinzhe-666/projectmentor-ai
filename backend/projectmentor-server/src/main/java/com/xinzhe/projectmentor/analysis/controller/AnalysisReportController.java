package com.xinzhe.projectmentor.analysis.controller;

import com.xinzhe.projectmentor.analysis.service.AnalysisReportService;
import com.xinzhe.projectmentor.analysis.vo.AnalysisReportVO;
import com.xinzhe.projectmentor.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AnalysisReportController {

    private final AnalysisReportService analysisReportService;

    @PostMapping("/api/projects/{projectId}/reports/generate")
    public Result<AnalysisReportVO> generateReport(@PathVariable Long projectId) {
        return Result.success(analysisReportService.generateReport(projectId));
    }

    @GetMapping("/api/projects/{projectId}/reports")
    public Result<List<AnalysisReportVO>> listProjectReports(@PathVariable Long projectId) {
        return Result.success(analysisReportService.listProjectReports(projectId));
    }

    @GetMapping("/api/reports/{reportId}")
    public Result<AnalysisReportVO> getReportDetail(@PathVariable Long reportId) {
        return Result.success(analysisReportService.getReportDetail(reportId));
    }
}
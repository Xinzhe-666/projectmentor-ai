package com.xinzhe.projectmentor.analysis.controller;

import com.xinzhe.projectmentor.analysis.service.AnalysisTaskService;
import com.xinzhe.projectmentor.analysis.vo.AnalysisTaskVO;
import com.xinzhe.projectmentor.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AnalysisTaskController {

    private final AnalysisTaskService analysisTaskService;

    @PostMapping("/api/projects/{projectId}/analyze")
    public Result<AnalysisTaskVO> startAnalysis(@PathVariable Long projectId) {
        return Result.success(analysisTaskService.startAnalysis(projectId));
    }

    @GetMapping("/api/tasks/{taskId}")
    public Result<AnalysisTaskVO> getTask(@PathVariable Long taskId) {
        return Result.success(analysisTaskService.getTask(taskId));
    }
}
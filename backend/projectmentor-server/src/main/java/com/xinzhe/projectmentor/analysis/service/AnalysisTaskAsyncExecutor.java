package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.vo.AnalysisReportVO;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisTaskAsyncExecutor {

    private final AnalysisReportService analysisReportService;

    private final TaskProgressService taskProgressService;

    @Async("analysisTaskExecutor")
    public void executeAnalysisTask(Long taskId, Long projectId, Long userId) {
        try {
            UserContext.setUserId(userId);

            taskProgressService.updateProgress(
                    taskId,
                    "RUNNING",
                    10,
                    "任务开始执行，正在准备项目审计",
                    null,
                    null,
                    false
            );

            taskProgressService.updateProgress(
                    taskId,
                    "RUNNING",
                    35,
                    "正在执行规则扫描和证据链分析",
                    null,
                    null,
                    false
            );

            AnalysisReportVO report = analysisReportService.generateReport(projectId);

            taskProgressService.updateProgress(
                    taskId,
                    "SUCCESS",
                    100,
                    "项目审计报告生成完成",
                    report.getId(),
                    null,
                    true
            );
        } catch (Exception e) {
            log.error("Analysis task failed, taskId={}, projectId={}", taskId, projectId, e);

            taskProgressService.updateProgress(
                    taskId,
                    "FAILED",
                    100,
                    "项目审计任务执行失败",
                    null,
                    e.getMessage(),
                    true
            );
        } finally {
            UserContext.clear();
        }
    }
}
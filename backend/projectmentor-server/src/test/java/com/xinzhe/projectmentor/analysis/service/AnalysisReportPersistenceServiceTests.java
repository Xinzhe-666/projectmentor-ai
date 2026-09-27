package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.credit.service.CreditService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisReportPersistenceServiceTests {

    @Test
    void reportGenerationAndClaimEnhancementAreNotLongTransactions() throws Exception {
        Method generateReport = AnalysisReportService.class.getMethod("generateReport", Long.class);
        Method enhanceClaimEvidence = AnalysisReportService.class.getMethod("enhanceClaimEvidence", Long.class);

        assertThat(generateReport.getAnnotation(Transactional.class)).isNull();
        assertThat(enhanceClaimEvidence.getAnnotation(Transactional.class)).isNull();
    }

    @Test
    void persistenceBoundaryHasShortTransaction() throws Exception {
        Method method = AnalysisReportPersistenceService.class.getMethod(
                "saveReportAndMarkProjectFinished",
                AnalysisReport.class,
                Project.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.rollbackFor()).containsExactly(Exception.class);
    }

    @Test
    void savesReportAndProjectStatusInsideOneBoundary() {
        AnalysisReportMapper reportMapper = mock(AnalysisReportMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        AnalysisReport report = new AnalysisReport();
        Project project = new Project();
        when(reportMapper.insert(report)).thenReturn(1);
        when(projectMapper.updateById(project)).thenReturn(1);

        new AnalysisReportPersistenceService(reportMapper, projectMapper, mock(AnalysisTaskMapper.class), mock(CreditService.class))
                .saveReportAndMarkProjectFinished(report, project);

        verify(reportMapper).insert(report);
        verify(projectMapper).updateById(project);
        assertThat(project.getStatus()).isEqualTo("FINISHED");
    }

    @Test
    void reportSaveFailureBecomesStableBusinessErrorAndSkipsProjectUpdate() {
        AnalysisReportMapper reportMapper = mock(AnalysisReportMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        AnalysisReport report = new AnalysisReport();
        Project project = new Project();
        when(reportMapper.insert(report)).thenReturn(0);

        assertThatThrownBy(() -> new AnalysisReportPersistenceService(
                reportMapper, projectMapper, mock(AnalysisTaskMapper.class), mock(CreditService.class))
                .saveReportAndMarkProjectFinished(report, project))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 审计报告已生成但保存失败，额度已返还，请稍后重试。");

        verify(projectMapper, never()).updateById(project);
    }

    @Test
    void projectStatusSaveFailureBecomesStableBusinessError() {
        AnalysisReportMapper reportMapper = mock(AnalysisReportMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        AnalysisReport report = new AnalysisReport();
        Project project = new Project();
        when(reportMapper.insert(report)).thenReturn(1);
        when(projectMapper.updateById(project)).thenReturn(0);

        assertThatThrownBy(() -> new AnalysisReportPersistenceService(
                reportMapper, projectMapper, mock(AnalysisTaskMapper.class), mock(CreditService.class))
                .saveReportAndMarkProjectFinished(report, project))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 审计报告状态保存失败，额度已返还，请稍后重试。");
    }

    @Test
    void staleFencingTokenCannotWriteReportOrProjectTerminalState() {
        AnalysisReportMapper reportMapper = mock(AnalysisReportMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        AnalysisTaskMapper taskMapper = mock(AnalysisTaskMapper.class);
        AnalysisExecutionContext stale = new AnalysisExecutionContext(
                100L, 42L, 7L, "old-worker", 2L, 1, "message-1", "correlation-1"
        );
        when(taskMapper.selectOwnedExecutionForUpdate(100L, "old-worker", 2L)).thenReturn(null);

        assertThatThrownBy(() -> new AnalysisReportPersistenceService(reportMapper, projectMapper, taskMapper, mock(CreditService.class))
                .saveReportAndCompleteTask(new AnalysisReport(), new Project(), stale))
                .isInstanceOf(StaleAnalysisExecutionException.class);

        verify(reportMapper, never()).insert(org.mockito.ArgumentMatchers.any(AnalysisReport.class));
        verify(projectMapper, never()).updateById(org.mockito.ArgumentMatchers.any(Project.class));
        verify(taskMapper, never()).completeSuccess(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void fallbackReportTaskProjectAndRefundShareOneShortTransactionBoundary() throws Exception {
        Method method = AnalysisReportPersistenceService.class.getMethod(
                "saveFallbackReportCompleteTaskAndRefund",
                AnalysisReport.class,
                Project.class,
                AnalysisExecutionContext.class
        );
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.rollbackFor()).containsExactly(Exception.class);

        AnalysisReportMapper reportMapper = mock(AnalysisReportMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        AnalysisTaskMapper taskMapper = mock(AnalysisTaskMapper.class);
        CreditService creditService = mock(CreditService.class);
        AnalysisExecutionContext execution = new AnalysisExecutionContext(
                100L, 42L, 7L, "worker-1", 3L, 4, "message-1", "correlation-1"
        );
        when(taskMapper.selectOwnedExecutionForUpdate(100L, "worker-1", 3L))
                .thenReturn(new com.xinzhe.projectmentor.analysis.entity.AnalysisTask());
        AnalysisReport report = new AnalysisReport();
        report.setId(900L);
        Project project = new Project();
        when(reportMapper.insert(report)).thenReturn(1);
        when(projectMapper.updateById(project)).thenReturn(1);
        when(taskMapper.completeSuccess(100L, "worker-1", 3L, 900L)).thenReturn(1);

        new AnalysisReportPersistenceService(reportMapper, projectMapper, taskMapper, creditService)
                .saveFallbackReportCompleteTaskAndRefund(report, project, execution);

        verify(taskMapper).selectOwnedExecutionForUpdate(100L, "worker-1", 3L);
        verify(reportMapper).insert(report);
        verify(projectMapper).updateById(project);
        verify(taskMapper).completeSuccess(100L, "worker-1", 3L, 900L);
        verify(creditService).refundCreditsOnceForTask(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.anyString()
        );
        assertThat(project.getStatus()).isEqualTo("FINISHED");
    }

    @Test
    void fallbackPersistenceFailureDoesNotAttemptTaskCompletionOrRefund() {
        AnalysisReportMapper reportMapper = mock(AnalysisReportMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        AnalysisTaskMapper taskMapper = mock(AnalysisTaskMapper.class);
        CreditService creditService = mock(CreditService.class);
        AnalysisExecutionContext execution = new AnalysisExecutionContext(
                100L, 42L, 7L, "worker-1", 3L, 4, "message-1", "correlation-1"
        );
        when(taskMapper.selectOwnedExecutionForUpdate(100L, "worker-1", 3L))
                .thenReturn(new com.xinzhe.projectmentor.analysis.entity.AnalysisTask());
        when(reportMapper.insert(org.mockito.ArgumentMatchers.any(AnalysisReport.class))).thenReturn(0);

        assertThatThrownBy(() -> new AnalysisReportPersistenceService(
                reportMapper, projectMapper, taskMapper, creditService
        ).saveFallbackReportCompleteTaskAndRefund(new AnalysisReport(), new Project(), execution))
                .isInstanceOf(BusinessException.class)
                .hasMessage("规则降级报告保存失败");

        verify(projectMapper, never()).updateById(org.mockito.ArgumentMatchers.any(Project.class));
        verify(taskMapper, never()).completeSuccess(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        );
        verify(creditService, never()).refundCreditsOnceForTask(
                org.mockito.ArgumentMatchers.any(Long.class),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Long.class),
                org.mockito.ArgumentMatchers.anyString()
        );
    }
}

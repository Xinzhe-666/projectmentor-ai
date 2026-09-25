package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
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

        new AnalysisReportPersistenceService(reportMapper, projectMapper)
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

        assertThatThrownBy(() -> new AnalysisReportPersistenceService(reportMapper, projectMapper)
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

        assertThatThrownBy(() -> new AnalysisReportPersistenceService(reportMapper, projectMapper)
                .saveReportAndMarkProjectFinished(report, project))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 审计报告状态保存失败，额度已返还，请稍后重试。");
    }
}

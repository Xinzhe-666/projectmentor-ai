package com.xinzhe.projectmentor.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.ai.AiJsonUtil;
import com.xinzhe.projectmentor.ai.LlmClient;
import com.xinzhe.projectmentor.ai.dto.AiAuditResult;
import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.claim.ClaimEvidenceAuditService;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceVO;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.credit.service.CreditService;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.scanner.ProjectRuleScanner;
import com.xinzhe.projectmentor.scanner.vo.RuleScanResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisReportServiceCreditTests {

    private CreditService creditService;
    private AnalysisReportPersistenceService persistenceService;
    private LlmClient llmClient;
    private AnalysisReportService service;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(7L);
        creditService = mock(CreditService.class);
        AnalysisReportMapper reportMapper = mock(AnalysisReportMapper.class);
        persistenceService = mock(AnalysisReportPersistenceService.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ProjectRuleScanner scanner = mock(ProjectRuleScanner.class);
        ClaimEvidenceAuditService claimAudit = mock(ClaimEvidenceAuditService.class);
        llmClient = mock(LlmClient.class);
        AuditPromptBuilder promptBuilder = mock(AuditPromptBuilder.class);
        ObjectMapper objectMapper = new ObjectMapper();

        Project project = new Project();
        project.setId(42L);
        project.setUserId(7L);
        project.setName("并发审计测试项目");
        project.setTechStack("Java 17");
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(project);
        when(scanner.scanProject(42L)).thenReturn(RuleScanResultVO.builder()
                .projectId(42L)
                .projectName(project.getName())
                .hasReadme(true)
                .fileCount(3)
                .totalRiskCount(0)
                .highRiskCount(0)
                .mediumRiskCount(0)
                .lowRiskCount(0)
                .risks(List.of())
                .evidences(List.of())
                .suggestions(List.of())
                .build());
        when(claimAudit.audit(project, 42L)).thenReturn(ClaimEvidenceVO.builder()
                .projectId(42L)
                .items(List.of())
                .build());
        when(promptBuilder.build(any(Project.class), any(RuleScanResultVO.class))).thenReturn("prompt");

        service = new AnalysisReportService(
                creditService,
                reportMapper,
                persistenceService,
                projectMapper,
                null,
                scanner,
                claimAudit,
                objectMapper,
                new AiJsonUtil(objectMapper),
                llmClient,
                promptBuilder,
                null
        );
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void aiFailureRefundsExactlyOnceAndPersistsFallbackReport() {
        when(llmClient.generateAuditReport("prompt")).thenThrow(new RuntimeException("AI unavailable"));

        var result = service.generateReport(42L);

        assertThat(result.getSummary()).contains("AI 调用失败").contains("额度已返还");
        verifyCreditConsumedOnce();
        verifyCreditRefundedOnce();
        verify(persistenceService).saveReportAndMarkProjectFinished(any(AnalysisReport.class), any(Project.class));
    }

    @Test
    void persistenceFailureAfterSuccessfulAiRefundsExactlyOnce() {
        when(llmClient.generateAuditReport("prompt")).thenReturn(successfulAiResult());
        doThrow(new BusinessException(ErrorCode.OPERATION_ERROR, "稳定保存错误"))
                .when(persistenceService)
                .saveReportAndMarkProjectFinished(any(AnalysisReport.class), any(Project.class));

        assertThatThrownBy(() -> service.generateReport(42L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("稳定保存错误");

        verifyCreditConsumedOnce();
        verifyCreditRefundedOnce();
    }

    @Test
    void successfulAiAndPersistenceDoNotRefundCredits() {
        when(llmClient.generateAuditReport("prompt")).thenReturn(successfulAiResult());

        service.generateReport(42L);

        verifyCreditConsumedOnce();
        verify(creditService, never()).refundCredits(any(), anyInt(), anyString(), any(), anyString());
    }

    private AiAuditResult successfulAiResult() {
        return AiAuditResult.builder()
                .summary("AI 总结")
                .strengths("优势")
                .weaknesses("不足")
                .suggestions("建议")
                .resumeBasic("保守版")
                .resumeStandard("标准版")
                .resumeAdvanced("进阶版")
                .build();
    }

    private void verifyCreditConsumedOnce() {
        verify(creditService, times(1)).consumeCredits(
                eq(7L),
                eq(CreditCostConstants.AI_AUDIT_REPORT),
                eq(CreditCostConstants.OP_AI_AUDIT_REPORT),
                eq(42L),
                anyString()
        );
    }

    private void verifyCreditRefundedOnce() {
        verify(creditService, times(1)).refundCredits(
                eq(7L),
                eq(CreditCostConstants.AI_AUDIT_REPORT),
                eq(CreditCostConstants.OP_AI_AUDIT_REPORT_REFUND),
                eq(42L),
                anyString()
        );
    }
}

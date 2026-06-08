package com.xinzhe.projectmentor.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.ai.AiJsonUtil;
import com.xinzhe.projectmentor.ai.LlmClient;
import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.analysis.vo.AnalysisReportVO;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.claim.ClaimCategory;
import com.xinzhe.projectmentor.claim.ClaimEvidenceAiPromptBuilder;
import com.xinzhe.projectmentor.claim.ClaimEvidenceAuditService;
import com.xinzhe.projectmentor.claim.ClaimEvidenceStatus;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.credit.service.CreditService;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisReportServiceClaimEvidenceAiTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void doesNotCallAiWhenCreditsAreInsufficient() throws Exception {
        TestFixture fixture = createFixture();
        doThrow(new BusinessException(ErrorCode.CREDIT_NOT_ENOUGH, "额度不足，当前剩余额度：1"))
                .when(fixture.creditService)
                .consumeCredits(
                        eq(7L),
                        eq(CreditCostConstants.AI_CLAIM_EVIDENCE),
                        eq(CreditCostConstants.OP_AI_CLAIM_EVIDENCE),
                        eq(100L),
                        anyString()
                );

        assertThatThrownBy(() -> fixture.service.enhanceClaimEvidence(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("额度不足");

        verify(fixture.llmClient, never()).chat(anyString(), anyString(), anyString());
        verify(fixture.creditService, never()).refundCredits(any(), anyInt(), anyString(), any(), anyString());
        verify(fixture.analysisReportMapper, never()).updateById(any(AnalysisReport.class));
    }

    @Test
    void refundsCreditsWhenAiCallFails() throws Exception {
        TestFixture fixture = createFixture();
        when(fixture.llmClient.chat(eq("CLAIM_EVIDENCE"), anyString(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 服务不可用"));

        assertThatThrownBy(() -> fixture.service.enhanceClaimEvidence(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("额度已返还");

        verify(fixture.creditService).refundCredits(
                eq(7L),
                eq(CreditCostConstants.AI_CLAIM_EVIDENCE),
                eq(CreditCostConstants.OP_AI_CLAIM_EVIDENCE_REFUND),
                eq(100L),
                contains("失败返还")
        );
        verify(fixture.analysisReportMapper, never()).updateById(any(AnalysisReport.class));
    }

    @Test
    void storesAiEnhancementInClaimEvidenceJson() throws Exception {
        TestFixture fixture = createFixture();
        when(fixture.llmClient.chat(eq("CLAIM_EVIDENCE"), anyString(), anyString()))
                .thenReturn("""
                        {
                          "summary": "整体应保守表达",
                          "riskOverview": "JWT 目前只有文档证据",
                          "resumeStrategy": "写成了解和设计，不写成完整落地",
                          "interviewStrategy": "准备解释 JWT 签发和拦截边界",
                          "items": [
                            {
                              "claimText": "项目支持 JWT 登录",
                              "aiExplanation": "当前状态是 DOC_ONLY，缺少代码证据",
                              "saferResumeExpression": "了解 JWT 登录设计",
                              "likelyInterviewQuestions": ["JWT 如何签发？"],
                              "improvementSuggestion": "补充 JwtUtil 和拦截器证据"
                            }
                          ]
                        }
                        """);
        when(fixture.analysisReportMapper.updateById(any(AnalysisReport.class))).thenReturn(1);

        AnalysisReportVO result = fixture.service.enhanceClaimEvidence(100L);

        assertThat(result.getClaimEvidenceAi()).isNotNull();
        assertThat(result.getClaimEvidenceAi().getAiSummary()).isEqualTo("整体应保守表达");
        assertThat(fixture.report.getClaimEvidence())
                .contains("\"aiEnhanced\":true")
                .contains("\"aiEnhancedItems\"");
        verify(fixture.creditService, never()).refundCredits(any(), anyInt(), anyString(), any(), anyString());
    }

    @Test
    void refundsCreditsWhenAiResultCannotBeSaved() throws Exception {
        TestFixture fixture = createFixture();
        when(fixture.llmClient.chat(eq("CLAIM_EVIDENCE"), anyString(), anyString()))
                .thenReturn("""
                        {
                          "summary": "整体应保守表达",
                          "riskOverview": "JWT 目前只有文档证据",
                          "resumeStrategy": "保守表达",
                          "interviewStrategy": "准备边界说明",
                          "items": []
                        }
                        """);
        when(fixture.analysisReportMapper.updateById(any(AnalysisReport.class))).thenReturn(0);

        assertThatThrownBy(() -> fixture.service.enhanceClaimEvidence(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("保存失败")
                .hasMessageContaining("额度已返还");

        verify(fixture.creditService).refundCredits(
                eq(7L),
                eq(CreditCostConstants.AI_CLAIM_EVIDENCE),
                eq(CreditCostConstants.OP_AI_CLAIM_EVIDENCE_REFUND),
                eq(100L),
                contains("失败返还")
        );
    }

    @SuppressWarnings("unchecked")
    private TestFixture createFixture() throws Exception {
        UserContext.setUserId(7L);

        AnalysisReportMapper analysisReportMapper = mock(AnalysisReportMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        CreditService creditService = mock(CreditService.class);
        LlmClient llmClient = mock(LlmClient.class);

        AnalysisReport report = new AnalysisReport();
        report.setId(100L);
        report.setProjectId(200L);
        report.setClaimEvidence(objectMapper.writeValueAsString(List.of(ClaimEvidenceItemVO.builder()
                .claimText("项目支持 JWT 登录")
                .sourceType("README")
                .category(ClaimCategory.AUTH)
                .status(ClaimEvidenceStatus.DOC_ONLY)
                .confidenceScore(35)
                .reason("当前仅 README 描述该能力")
                .evidenceFiles(List.of())
                .resumeAdvice("建议保守写成了解 JWT 登录设计")
                .interviewQuestion("JWT 登录如何签发和校验？")
                .build())));

        Project project = new Project();
        project.setId(200L);
        project.setUserId(7L);

        when(analysisReportMapper.selectById(100L)).thenReturn(report);
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(project);

        ClaimEvidenceAuditService claimEvidenceAuditService =
                new ClaimEvidenceAuditService(null, null, null, objectMapper);
        AnalysisReportService service = new AnalysisReportService(
                creditService,
                analysisReportMapper,
                projectMapper,
                null,
                null,
                claimEvidenceAuditService,
                objectMapper,
                new AiJsonUtil(objectMapper),
                llmClient,
                null,
                new ClaimEvidenceAiPromptBuilder(objectMapper)
        );

        return new TestFixture(service, analysisReportMapper, creditService, llmClient, report);
    }

    private record TestFixture(AnalysisReportService service,
                               AnalysisReportMapper analysisReportMapper,
                               CreditService creditService,
                               LlmClient llmClient,
                               AnalysisReport report) {
    }
}

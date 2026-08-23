package com.xinzhe.projectmentor.defense.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.claim.ClaimCategory;
import com.xinzhe.projectmentor.claim.ClaimEvidenceAuditService;
import com.xinzhe.projectmentor.claim.ClaimEvidenceStatus;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceFileVO;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO;
import com.xinzhe.projectmentor.defense.dto.CreateDefenseSessionRequest;
import com.xinzhe.projectmentor.defense.dto.DefenseAnswerResponse;
import com.xinzhe.projectmentor.defense.dto.DefenseEvidenceReference;
import com.xinzhe.projectmentor.defense.dto.DefenseReviewResult;
import com.xinzhe.projectmentor.defense.dto.DefenseSessionResponse;
import com.xinzhe.projectmentor.defense.dto.DefenseSessionReviewResponse;
import com.xinzhe.projectmentor.defense.dto.SubmitDefenseAnswerRequest;
import com.xinzhe.projectmentor.defense.entity.DefenseAnswer;
import com.xinzhe.projectmentor.defense.entity.DefenseQuestion;
import com.xinzhe.projectmentor.defense.entity.DefenseSession;
import com.xinzhe.projectmentor.defense.mapper.DefenseAnswerMapper;
import com.xinzhe.projectmentor.defense.mapper.DefenseQuestionMapper;
import com.xinzhe.projectmentor.defense.mapper.DefenseSessionMapper;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefenseServiceTests {

    private static final Long USER_ID = 7L;

    private static final Long PROJECT_ID = 42L;

    private static final Long REPORT_ID = 84L;

    @BeforeAll
    static void initializeMybatisPlusLambdaMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        List.of(
                Project.class,
                DefenseSession.class,
                DefenseQuestion.class,
                DefenseAnswer.class
        ).forEach(entityType -> initializeTableInfo(configuration, entityType));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createsSessionAndGeneratesQuestionsFromClaimEvidence() {
        TestFixture fixture = fixture();
        ClaimEvidenceItemVO claim = supportedClaim();
        when(fixture.claimEvidenceAuditService.parseItems(any())).thenReturn(List.of(claim));

        DefenseSessionResponse response = fixture.service.createSession(PROJECT_ID, createRequest());

        assertThat(response.getStatus()).isEqualTo(DefenseService.STATUS_ACTIVE);
        assertThat(response.getQuestionCount()).isEqualTo(1);
        assertThat(response.getMode()).isEqualTo(DefenseService.MODE_EVIDENCE_DEFENSE);

        ArgumentCaptor<DefenseQuestion> questionCaptor = ArgumentCaptor.forClass(DefenseQuestion.class);
        verify(fixture.defenseQuestionMapper).insert(questionCaptor.capture());
        DefenseQuestion question = questionCaptor.getValue();
        assertThat(question.getCategory()).isEqualTo("AUTH");
        assertThat(question.getQuestion()).contains("JWT 登录鉴权", "具体文件");
        assertThat(question.getRelatedClaims()).contains("JWT 登录鉴权");
        assertThat(question.getRelatedEvidence()).contains("src/AuthService.java");
    }

    @Test
    void createsExplicitInsufficientDataSessionWhenReportHasNoClaimsOrRisks() {
        TestFixture fixture = fixture();
        when(fixture.claimEvidenceAuditService.parseItems(any())).thenReturn(List.of());
        fixture.report.setRiskPoints(null);

        DefenseSessionResponse response = fixture.service.createSession(PROJECT_ID, createRequest());

        assertThat(response.getStatus()).isEqualTo(DefenseService.STATUS_INSUFFICIENT_DATA);
        assertThat(response.getQuestionCount()).isZero();
        verify(fixture.defenseQuestionMapper, never()).insert(any(DefenseQuestion.class));
    }

    @Test
    void generatesRiskQuestionWhenClaimMatrixIsEmpty() {
        TestFixture fixture = fixture();
        when(fixture.claimEvidenceAuditService.parseItems(any())).thenReturn(List.of());
        fixture.report.setRiskPoints("""
                [
                  {
                    "riskLevel": "HIGH",
                    "riskType": "README_OVERCLAIM",
                    "sourceFile": "README.md",
                    "message": "README 声明了高并发，但缺少实现证据",
                    "evidence": "当前只发现 README 描述"
                  }
                ]
                """);

        DefenseSessionResponse response = fixture.service.createSession(PROJECT_ID, createRequest());

        assertThat(response.getStatus()).isEqualTo(DefenseService.STATUS_ACTIVE);
        assertThat(response.getQuestionCount()).isEqualTo(1);
        ArgumentCaptor<DefenseQuestion> questionCaptor = ArgumentCaptor.forClass(DefenseQuestion.class);
        verify(fixture.defenseQuestionMapper).insert(questionCaptor.capture());
        assertThat(questionCaptor.getValue().getCategory()).isEqualTo("RISK");
        assertThat(questionCaptor.getValue().getQuestion()).contains("高并发", "项目证据");
        assertThat(questionCaptor.getValue().getRelatedEvidence()).contains("README.md");
    }

    @Test
    void submitAnswerUsesBoundEvidenceAndCompletesTheSession() throws Exception {
        TestFixture fixture = fixture();
        DefenseSession session = session(100L, DefenseService.STATUS_ACTIVE, 1);
        DefenseQuestion question = question(200L, session.getId());
        question.setRelatedClaims(fixture.objectMapper.writeValueAsString(List.of("项目实现 JWT 登录鉴权")));
        question.setRelatedEvidence(fixture.objectMapper.writeValueAsString(List.of(
                DefenseEvidenceReference.builder()
                        .fileId(11L)
                        .filePath("src/AuthService.java")
                        .evidenceLevel("STRONG")
                        .build()
        )));

        when(fixture.defenseQuestionMapper.selectById(question.getId())).thenReturn(question);
        when(fixture.defenseSessionMapper.selectById(session.getId())).thenReturn(session);
        when(fixture.defenseAnswerMapper.selectCount(any())).thenReturn(0L, 1L);
        when(fixture.defenseQuestionMapper.selectList(any())).thenReturn(List.of(question));

        SubmitDefenseAnswerRequest request = new SubmitDefenseAnswerRequest();
        request.setAnswerText("登录流程由 src/AuthService.java 负责校验并签发 token。");

        DefenseAnswerResponse response = fixture.service.submitAnswer(question.getId(), request);

        assertThat(response.getEvaluationStatus()).isEqualTo(DefenseService.ALIGNMENT_SUPPORTED);
        assertThat(response.getReviewResult().getMatchedEvidence())
                .extracting(DefenseEvidenceReference::getFilePath)
                .containsExactly("src/AuthService.java");
        assertThat(session.getStatus()).isEqualTo(DefenseService.STATUS_COMPLETED);
        verify(fixture.defenseSessionMapper).updateById(session);
    }

    @Test
    void submitAnswerIsPartialWhenBoundEvidenceIsNotExplicitlyReferenced() throws Exception {
        TestFixture fixture = fixture();
        DefenseSession session = session(100L, DefenseService.STATUS_ACTIVE, 1);
        DefenseQuestion question = question(200L, session.getId());
        question.setRelatedEvidence(fixture.objectMapper.writeValueAsString(List.of(
                DefenseEvidenceReference.builder()
                        .fileId(11L)
                        .filePath("src/AuthService.java")
                        .evidenceLevel("STRONG")
                        .build()
        )));
        prepareAnswerSubmission(fixture, session, question);

        SubmitDefenseAnswerRequest request = new SubmitDefenseAnswerRequest();
        request.setAnswerText("我实现了登录流程，但这里没有指出具体证据文件。");

        DefenseAnswerResponse response = fixture.service.submitAnswer(question.getId(), request);

        assertThat(response.getEvaluationStatus()).isEqualTo(DefenseService.ALIGNMENT_PARTIAL);
        assertThat(response.getReviewResult().getMatchedEvidence()).isEmpty();
    }

    @Test
    void submitAnswerIsInsufficientWhenQuestionHasNoBoundEvidence() {
        TestFixture fixture = fixture();
        DefenseSession session = session(100L, DefenseService.STATUS_ACTIVE, 1);
        DefenseQuestion question = question(200L, session.getId());
        prepareAnswerSubmission(fixture, session, question);

        SubmitDefenseAnswerRequest request = new SubmitDefenseAnswerRequest();
        request.setAnswerText("当前报告没有可验证的文件证据。");

        DefenseAnswerResponse response = fixture.service.submitAnswer(question.getId(), request);

        assertThat(response.getEvaluationStatus()).isEqualTo(DefenseService.ALIGNMENT_INSUFFICIENT);
        assertThat(response.getReviewResult().getSummary()).contains("没有可验证的关联证据");
    }

    @Test
    void buildsSessionReviewFromPersistedRuleResults() throws Exception {
        TestFixture fixture = fixture();
        DefenseSession session = session(100L, DefenseService.STATUS_COMPLETED, 1);
        DefenseQuestion question = question(200L, session.getId());
        question.setRelatedClaims("[]");
        question.setRelatedEvidence("[]");

        DefenseReviewResult reviewResult = DefenseReviewResult.builder()
                .evidenceAlignment(DefenseService.ALIGNMENT_PARTIAL)
                .summary("部分对齐")
                .relatedClaims(List.of())
                .matchedEvidence(List.of())
                .build();
        DefenseAnswer answer = new DefenseAnswer();
        answer.setId(300L);
        answer.setQuestionId(question.getId());
        answer.setAnswerText("回答内容");
        answer.setEvaluationStatus(DefenseService.ALIGNMENT_PARTIAL);
        answer.setReviewResult(fixture.objectMapper.writeValueAsString(reviewResult));

        when(fixture.defenseSessionMapper.selectById(session.getId())).thenReturn(session);
        when(fixture.defenseQuestionMapper.selectList(any())).thenReturn(List.of(question));
        when(fixture.defenseAnswerMapper.selectList(any())).thenReturn(List.of(answer));

        DefenseSessionReviewResponse response = fixture.service.getSessionReview(session.getId());

        assertThat(response.getAnsweredCount()).isEqualTo(1);
        assertThat(response.getSupportedCount()).isZero();
        assertThat(response.getPartialCount()).isEqualTo(1);
        assertThat(response.getInsufficientCount()).isZero();
        assertThat(response.getQuestions()).hasSize(1);
        assertThat(response.getQuestions().get(0).getAnswer().getReviewResult().getSummary())
                .isEqualTo("部分对齐");
    }

    private TestFixture fixture() {
        UserContext.setUserId(USER_ID);
        DefenseSessionMapper defenseSessionMapper = mock(DefenseSessionMapper.class);
        DefenseQuestionMapper defenseQuestionMapper = mock(DefenseQuestionMapper.class);
        DefenseAnswerMapper defenseAnswerMapper = mock(DefenseAnswerMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        AnalysisReportMapper analysisReportMapper = mock(AnalysisReportMapper.class);
        ClaimEvidenceAuditService claimEvidenceAuditService = mock(ClaimEvidenceAuditService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DefenseService service = new DefenseService(
                defenseSessionMapper,
                defenseQuestionMapper,
                defenseAnswerMapper,
                projectMapper,
                analysisReportMapper,
                claimEvidenceAuditService,
                objectMapper
        );

        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setUserId(USER_ID);
        AnalysisReport report = new AnalysisReport();
        report.setId(REPORT_ID);
        report.setProjectId(PROJECT_ID);
        report.setClaimEvidence("{}");

        when(projectMapper.selectOne(any())).thenReturn(project);
        when(analysisReportMapper.selectById(REPORT_ID)).thenReturn(report);
        when(defenseSessionMapper.insert(any(DefenseSession.class))).thenAnswer(invocation -> {
            DefenseSession session = invocation.getArgument(0);
            session.setId(100L);
            return 1;
        });
        AtomicLong questionIds = new AtomicLong(200L);
        when(defenseQuestionMapper.insert(any(DefenseQuestion.class))).thenAnswer(invocation -> {
            DefenseQuestion question = invocation.getArgument(0);
            question.setId(questionIds.getAndIncrement());
            return 1;
        });
        when(defenseAnswerMapper.insert(any(DefenseAnswer.class))).thenAnswer(invocation -> {
            DefenseAnswer answer = invocation.getArgument(0);
            answer.setId(300L);
            return 1;
        });
        when(defenseSessionMapper.updateById(any(DefenseSession.class))).thenReturn(1);

        return new TestFixture(
                service,
                defenseSessionMapper,
                defenseQuestionMapper,
                defenseAnswerMapper,
                claimEvidenceAuditService,
                objectMapper,
                report
        );
    }

    private CreateDefenseSessionRequest createRequest() {
        CreateDefenseSessionRequest request = new CreateDefenseSessionRequest();
        request.setReportId(REPORT_ID);
        return request;
    }

    private ClaimEvidenceItemVO supportedClaim() {
        return ClaimEvidenceItemVO.builder()
                .claimText("项目实现 JWT 登录鉴权")
                .sourceType("README")
                .category(ClaimCategory.AUTH)
                .status(ClaimEvidenceStatus.SUPPORTED)
                .evidenceFiles(List.of(
                        ClaimEvidenceFileVO.builder()
                                .fileId(11L)
                                .filePath("src/AuthService.java")
                                .fileType("CODE")
                                .evidenceLevel("STRONG")
                                .snippet("class AuthService")
                                .reason("存在登录实现")
                                .build()
                ))
                .build();
    }

    private DefenseSession session(Long id, String status, int questionCount) {
        DefenseSession session = new DefenseSession();
        session.setId(id);
        session.setProjectId(PROJECT_ID);
        session.setReportId(REPORT_ID);
        session.setMode(DefenseService.MODE_EVIDENCE_DEFENSE);
        session.setStatus(status);
        session.setQuestionCount(questionCount);
        return session;
    }

    private DefenseQuestion question(Long id, Long sessionId) {
        DefenseQuestion question = new DefenseQuestion();
        question.setId(id);
        question.setSessionId(sessionId);
        question.setQuestion("请说明登录鉴权实现。");
        question.setCategory("AUTH");
        question.setRelatedClaims("[]");
        question.setRelatedEvidence("[]");
        question.setSortOrder(1);
        return question;
    }

    private void prepareAnswerSubmission(TestFixture fixture,
                                         DefenseSession session,
                                         DefenseQuestion question) {
        when(fixture.defenseQuestionMapper.selectById(question.getId())).thenReturn(question);
        when(fixture.defenseSessionMapper.selectById(session.getId())).thenReturn(session);
        when(fixture.defenseAnswerMapper.selectCount(any())).thenReturn(0L, 1L);
        when(fixture.defenseQuestionMapper.selectList(any())).thenReturn(List.of(question));
    }

    private static void initializeTableInfo(MybatisConfiguration configuration, Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) != null) {
            return;
        }
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, entityType.getName());
        assistant.setCurrentNamespace(entityType.getName());
        TableInfoHelper.initTableInfo(assistant, entityType);
    }

    private record TestFixture(
            DefenseService service,
            DefenseSessionMapper defenseSessionMapper,
            DefenseQuestionMapper defenseQuestionMapper,
            DefenseAnswerMapper defenseAnswerMapper,
            ClaimEvidenceAuditService claimEvidenceAuditService,
            ObjectMapper objectMapper,
            AnalysisReport report
    ) {
    }
}

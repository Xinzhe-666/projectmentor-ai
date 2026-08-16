package com.xinzhe.projectmentor.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisReportMapper;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.interview.entity.InterviewMessage;
import com.xinzhe.projectmentor.interview.entity.InterviewSession;
import com.xinzhe.projectmentor.interview.mapper.InterviewMessageMapper;
import com.xinzhe.projectmentor.interview.mapper.InterviewSessionMapper;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import com.xinzhe.projectmentor.qa.mapper.ProjectQaRecordMapper;
import com.xinzhe.projectmentor.share.entity.ReportShare;
import com.xinzhe.projectmentor.share.mapper.ReportShareMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectServiceTests {

    private static final Long USER_ID = 7L;
    private static final Long PROJECT_ID = 42L;

    @BeforeAll
    static void initializeMybatisPlusLambdaMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        List.of(
                Project.class,
                ProjectFile.class,
                AnalysisTask.class,
                AnalysisReport.class,
                ReportShare.class,
                InterviewSession.class,
                InterviewMessage.class
        ).forEach(entityType -> initializeTableInfo(configuration, entityType));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void deleteOwnedProjectCleansAllDerivedDataInDependencyOrder() {
        TestFixture fixture = ownedProjectFixture();
        when(fixture.analysisReportMapper.selectList(any())).thenReturn(List.of(
                report(101L), report(102L)
        ));
        when(fixture.interviewSessionMapper.selectList(any())).thenReturn(List.of(
                session(201L), session(202L)
        ));

        fixture.service.deleteProject(PROJECT_ID);

        ArgumentCaptor<LambdaQueryWrapper> shareDelete = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        ArgumentCaptor<LambdaQueryWrapper> messageDelete = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(fixture.reportShareMapper).delete(shareDelete.capture());
        verify(fixture.interviewMessageMapper).delete(messageDelete.capture());
        assertThat(boundValues(shareDelete.getValue())).contains(101L, 102L);
        assertThat(boundValues(messageDelete.getValue())).contains(201L, 202L);

        InOrder deleteOrder = inOrder(
                fixture.reportShareMapper,
                fixture.interviewMessageMapper,
                fixture.projectQaRecordMapper,
                fixture.analysisTaskMapper,
                fixture.analysisReportMapper,
                fixture.interviewSessionMapper,
                fixture.projectFileMapper,
                fixture.projectMapper
        );
        deleteOrder.verify(fixture.reportShareMapper).delete(any());
        deleteOrder.verify(fixture.interviewMessageMapper).delete(any());
        deleteOrder.verify(fixture.projectQaRecordMapper).physicalDeleteByProjectId(PROJECT_ID);
        deleteOrder.verify(fixture.analysisTaskMapper).delete(any());
        deleteOrder.verify(fixture.analysisReportMapper).delete(any());
        deleteOrder.verify(fixture.interviewSessionMapper).delete(any());
        deleteOrder.verify(fixture.projectFileMapper).delete(any());
        deleteOrder.verify(fixture.projectMapper).deleteById(PROJECT_ID);
    }

    @Test
    void missingProjectReturnsNotFoundWithoutDeletingChildren() {
        TestFixture fixture = fixture();
        when(fixture.projectMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> fixture.service.deleteProject(PROJECT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不存在或无权限删除")
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_FOUND.getCode());

        verifyNoChildInteractions(fixture);
        verify(fixture.projectMapper, never()).deleteById(anyLong());
    }

    @Test
    void anotherUsersProjectUsesOwnerScopedLookupAndDoesNotLeakExistence() {
        TestFixture fixture = fixture();
        ArgumentCaptor<LambdaQueryWrapper> ownerLookup = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(fixture.projectMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> fixture.service.deleteProject(PROJECT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不存在或无权限删除");

        verify(fixture.projectMapper).selectOne(ownerLookup.capture());
        assertThat(boundValues(ownerLookup.getValue())).contains(USER_ID, PROJECT_ID);
        verifyNoChildInteractions(fixture);
    }

    @Test
    void pendingAnalysisTaskBlocksDeletion() {
        assertActiveStatusBlocksDeletion("PENDING");
    }

    @Test
    void runningAnalysisTaskBlocksDeletion() {
        assertActiveStatusBlocksDeletion("RUNNING");
    }

    @Test
    void successfulAndFailedHistoricalTasksDoNotBlockDeletion() {
        TestFixture fixture = ownedProjectFixture();
        ArgumentCaptor<LambdaQueryWrapper> activeLookup = ArgumentCaptor.forClass(LambdaQueryWrapper.class);

        fixture.service.deleteProject(PROJECT_ID);

        verify(fixture.analysisTaskMapper).selectCount(activeLookup.capture());
        Collection<Object> values = boundValues(activeLookup.getValue());
        assertThat(values).contains("PENDING", "RUNNING");
        assertThat(values).doesNotContain("SUCCESS", "FAILED");
        verify(fixture.analysisTaskMapper).delete(any());
        verify(fixture.projectMapper).deleteById(PROJECT_ID);
    }

    @Test
    void noReportsSkipsShareDeleteAndNeverBuildsAnEmptyInClause() {
        TestFixture fixture = ownedProjectFixture();
        when(fixture.analysisReportMapper.selectList(any())).thenReturn(List.of());

        fixture.service.deleteProject(PROJECT_ID);

        verify(fixture.reportShareMapper, never()).delete(any());
        verify(fixture.analysisReportMapper).delete(any());
    }

    @Test
    void noInterviewSessionsSkipsMessageDeleteAndNeverBuildsAnEmptyInClause() {
        TestFixture fixture = ownedProjectFixture();
        when(fixture.interviewSessionMapper.selectList(any())).thenReturn(List.of());

        fixture.service.deleteProject(PROJECT_ID);

        verify(fixture.interviewMessageMapper, never()).delete(any());
        verify(fixture.interviewSessionMapper).delete(any());
    }

    @Test
    void finalProjectDeleteFailureReturnsStableBusinessError() {
        TestFixture fixture = ownedProjectFixture();
        when(fixture.projectMapper.deleteById(PROJECT_ID)).thenReturn(0);

        assertThatThrownBy(() -> fixture.service.deleteProject(PROJECT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目删除失败，请稍后重试")
                .extracting("code")
                .isEqualTo(ErrorCode.OPERATION_ERROR.getCode());

        verify(fixture.projectFileMapper).delete(any());
        verify(fixture.projectMapper).deleteById(PROJECT_ID);
    }

    @Test
    void childDeleteExceptionStopsTheWorkflowBeforeProjectDeletion() {
        TestFixture fixture = ownedProjectFixture();
        doThrow(new RuntimeException("database delete failed"))
                .when(fixture.analysisReportMapper)
                .delete(any());

        assertThatThrownBy(() -> fixture.service.deleteProject(PROJECT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("database delete failed");

        verify(fixture.projectQaRecordMapper).physicalDeleteByProjectId(PROJECT_ID);
        verify(fixture.analysisTaskMapper).delete(any());
        verify(fixture.interviewSessionMapper, never()).delete(any());
        verify(fixture.projectFileMapper, never()).delete(any());
        verify(fixture.projectMapper, never()).deleteById(anyLong());
    }

    @Test
    void deletionTransactionRollsBackForAnyException() throws Exception {
        Method deleteProject = ProjectService.class.getMethod("deleteProject", Long.class);
        Transactional transactional = deleteProject.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.rollbackFor()).containsExactly(Exception.class);
    }

    @Test
    void qaPhysicalDeleteUsesAProjectIdBoundDeleteStatement() throws Exception {
        Method physicalDelete = ProjectQaRecordMapper.class
                .getMethod("physicalDeleteByProjectId", Long.class);
        Delete delete = physicalDelete.getAnnotation(Delete.class);
        Annotation[] parameterAnnotations = physicalDelete.getParameterAnnotations()[0];

        assertThat(delete).isNotNull();
        assertThat(delete.value()).containsExactly(
                "DELETE FROM pm_project_qa_record WHERE project_id = #{projectId}"
        );
        assertThat(parameterAnnotations)
                .filteredOn(annotation -> annotation instanceof Param)
                .singleElement()
                .satisfies(annotation -> assertThat(((Param) annotation).value()).isEqualTo("projectId"));
    }

    private void assertActiveStatusBlocksDeletion(String activeStatus) {
        TestFixture fixture = ownedProjectFixture();
        when(fixture.analysisTaskMapper.selectCount(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<?> query = invocation.getArgument(0);
            return boundValues(query).contains(activeStatus) ? 1L : 0L;
        });

        assertThatThrownBy(() -> fixture.service.deleteProject(PROJECT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目正在分析中，请等待分析完成后再删除")
                .extracting("code")
                .isEqualTo(ErrorCode.OPERATION_ERROR.getCode());

        verify(fixture.analysisReportMapper, never()).selectList(any());
        verify(fixture.interviewSessionMapper, never()).selectList(any());
        verify(fixture.projectQaRecordMapper, never()).physicalDeleteByProjectId(any());
        verify(fixture.analysisTaskMapper, never()).delete(any());
        verify(fixture.projectMapper, never()).deleteById(anyLong());
    }

    private static Collection<Object> boundValues(LambdaQueryWrapper<?> query) {
        query.getSqlSegment();
        return List.copyOf(query.getParamNameValuePairs().values());
    }

    private static void initializeTableInfo(MybatisConfiguration configuration, Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) != null) {
            return;
        }
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, entityType.getName());
        assistant.setCurrentNamespace(entityType.getName());
        TableInfoHelper.initTableInfo(assistant, entityType);
    }

    private static AnalysisReport report(Long id) {
        AnalysisReport report = new AnalysisReport();
        report.setId(id);
        report.setProjectId(PROJECT_ID);
        return report;
    }

    private static InterviewSession session(Long id) {
        InterviewSession session = new InterviewSession();
        session.setId(id);
        session.setProjectId(PROJECT_ID);
        return session;
    }

    private static TestFixture ownedProjectFixture() {
        TestFixture fixture = fixture();
        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setUserId(USER_ID);
        when(fixture.projectMapper.selectOne(any())).thenReturn(project);
        when(fixture.analysisTaskMapper.selectCount(any())).thenReturn(0L);
        when(fixture.analysisReportMapper.selectList(any())).thenReturn(List.of());
        when(fixture.interviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(fixture.projectMapper.deleteById(PROJECT_ID)).thenReturn(1);
        return fixture;
    }

    private static TestFixture fixture() {
        UserContext.setUserId(USER_ID);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ProjectFileMapper projectFileMapper = mock(ProjectFileMapper.class);
        ProjectQaRecordMapper projectQaRecordMapper = mock(ProjectQaRecordMapper.class);
        AnalysisTaskMapper analysisTaskMapper = mock(AnalysisTaskMapper.class);
        AnalysisReportMapper analysisReportMapper = mock(AnalysisReportMapper.class);
        ReportShareMapper reportShareMapper = mock(ReportShareMapper.class);
        InterviewSessionMapper interviewSessionMapper = mock(InterviewSessionMapper.class);
        InterviewMessageMapper interviewMessageMapper = mock(InterviewMessageMapper.class);
        ProjectService service = new ProjectService(
                projectMapper,
                projectFileMapper,
                projectQaRecordMapper,
                analysisTaskMapper,
                analysisReportMapper,
                reportShareMapper,
                interviewSessionMapper,
                interviewMessageMapper
        );
        return new TestFixture(
                service,
                projectMapper,
                projectFileMapper,
                projectQaRecordMapper,
                analysisTaskMapper,
                analysisReportMapper,
                reportShareMapper,
                interviewSessionMapper,
                interviewMessageMapper
        );
    }

    private static void verifyNoChildInteractions(TestFixture fixture) {
        verifyNoInteractions(
                fixture.projectFileMapper,
                fixture.projectQaRecordMapper,
                fixture.analysisTaskMapper,
                fixture.analysisReportMapper,
                fixture.reportShareMapper,
                fixture.interviewSessionMapper,
                fixture.interviewMessageMapper
        );
    }

    private record TestFixture(ProjectService service,
                               ProjectMapper projectMapper,
                               ProjectFileMapper projectFileMapper,
                               ProjectQaRecordMapper projectQaRecordMapper,
                               AnalysisTaskMapper analysisTaskMapper,
                               AnalysisReportMapper analysisReportMapper,
                               ReportShareMapper reportShareMapper,
                               InterviewSessionMapper interviewSessionMapper,
                               InterviewMessageMapper interviewMessageMapper) {
    }
}

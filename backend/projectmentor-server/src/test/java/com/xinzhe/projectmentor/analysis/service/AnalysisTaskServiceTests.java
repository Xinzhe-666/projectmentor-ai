package com.xinzhe.projectmentor.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.analysis.vo.AnalysisTaskVO;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisTaskServiceTests {

    private AnalysisTaskMapper analysisTaskMapper;
    private ProjectMapper projectMapper;
    private AnalysisTaskAsyncExecutor asyncExecutor;
    private TaskProgressService taskProgressService;
    private AnalysisTaskService service;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(7L);
        analysisTaskMapper = mock(AnalysisTaskMapper.class);
        projectMapper = mock(ProjectMapper.class);
        asyncExecutor = mock(AnalysisTaskAsyncExecutor.class);
        taskProgressService = mock(TaskProgressService.class);
        service = new AnalysisTaskService(
                analysisTaskMapper,
                projectMapper,
                asyncExecutor,
                taskProgressService
        );

        Project project = new Project();
        project.setId(42L);
        project.setUserId(7L);
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(project);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createsActiveTaskWithExpectedKey() {
        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            AnalysisTask task = invocation.getArgument(0);
            task.setId(100L);
            return 1;
        }).when(analysisTaskMapper).insert(any(AnalysisTask.class));
        AnalysisTaskVO expected = taskVo(100L, "PENDING", 0);
        when(taskProgressService.getProgress(100L)).thenReturn(expected);

        AnalysisTaskVO result = service.startAnalysis(42L);

        ArgumentCaptor<AnalysisTask> taskCaptor = ArgumentCaptor.forClass(AnalysisTask.class);
        verify(analysisTaskMapper).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getActiveKey()).isEqualTo("FULL_ANALYSIS:42");
        assertThat(taskCaptor.getValue().getStatus()).isEqualTo("PENDING");
        verify(asyncExecutor).executeAnalysisTask(100L, 42L, 7L);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void duplicateRequestReturnsExistingActiveTask() {
        AnalysisTask existing = activeTask(88L);
        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        AnalysisTaskVO expected = taskVo(88L, "RUNNING", 35);
        when(taskProgressService.getProgress(88L)).thenReturn(expected);

        AnalysisTaskVO result = service.startAnalysis(42L);

        assertThat(result).isSameAs(expected);
        verify(analysisTaskMapper, never()).insert(any(AnalysisTask.class));
        verify(asyncExecutor, never()).executeAnalysisTask(any(), any(), any());
    }

    @Test
    void duplicateKeyRaceReturnsConcurrentActiveTask() {
        AnalysisTask concurrent = activeTask(89L);
        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null, concurrent);
        doThrow(new DuplicateKeyException("duplicate active key"))
                .when(analysisTaskMapper).insert(any(AnalysisTask.class));
        AnalysisTaskVO expected = taskVo(89L, "PENDING", 0);
        when(taskProgressService.getProgress(89L)).thenReturn(expected);

        AnalysisTaskVO result = service.startAnalysis(42L);

        assertThat(result).isSameAs(expected);
        verify(asyncExecutor, never()).executeAnalysisTask(any(), any(), any());
    }

    @Test
    void duplicateKeyWithoutRecoverableTaskBecomesStableBusinessError() {
        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doThrow(new DuplicateKeyException("uk_analysis_task_active_key"))
                .when(analysisTaskMapper).insert(any(AnalysisTask.class));

        assertThatThrownBy(() -> service.startAnalysis(42L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("审计任务创建失败，请稍后重试")
                .hasMessageNotContaining("uk_analysis_task_active_key");
    }

    @Test
    void rejectedExecutionMarksTaskFailedInsteadOfLeavingPending() {
        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            AnalysisTask task = invocation.getArgument(0);
            task.setId(101L);
            return 1;
        }).when(analysisTaskMapper).insert(any(AnalysisTask.class));
        doThrow(new TaskRejectedException("executor saturated"))
                .when(asyncExecutor).executeAnalysisTask(101L, 42L, 7L);

        assertThatThrownBy(() -> service.startAnalysis(42L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前审计任务较多，请稍后重试")
                .hasMessageNotContaining("executor saturated");

        verify(taskProgressService).updateProgress(
                eq(101L),
                eq("FAILED"),
                eq(100),
                eq("当前审计任务较多，请稍后重试"),
                eq(null),
                eq("本地审计执行队列已满"),
                eq(true)
        );
    }

    private AnalysisTask activeTask(Long id) {
        AnalysisTask task = new AnalysisTask();
        task.setId(id);
        task.setUserId(7L);
        task.setProjectId(42L);
        task.setTaskType("FULL_ANALYSIS");
        task.setActiveKey("FULL_ANALYSIS:42");
        task.setStatus("PENDING");
        task.setProgress(0);
        return task;
    }

    private AnalysisTaskVO taskVo(Long id, String status, Integer progress) {
        return AnalysisTaskVO.builder()
                .taskId(id)
                .projectId(42L)
                .taskType("FULL_ANALYSIS")
                .status(status)
                .progress(progress)
                .build();
    }
}

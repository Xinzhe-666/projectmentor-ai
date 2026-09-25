package com.xinzhe.projectmentor.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.analysis.vo.AnalysisTaskVO;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisTaskService {

    private static final String FULL_ANALYSIS = "FULL_ANALYSIS";

    private static final String ACTIVE_KEY_PREFIX = FULL_ANALYSIS + ":";

    private final AnalysisTaskMapper analysisTaskMapper;

    private final ProjectMapper projectMapper;

    private final AnalysisTaskAsyncExecutor asyncExecutor;

    private final TaskProgressService taskProgressService;

    public AnalysisTaskVO startAnalysis(Long projectId) {
        Long userId = getCurrentUserId();

        checkProjectOwner(projectId, userId);

        String activeKey = buildActiveKey(projectId);
        AnalysisTask existingTask = findActiveTask(userId, projectId, activeKey);
        if (existingTask != null) {
            return taskProgressService.getProgress(existingTask.getId());
        }

        AnalysisTask task = new AnalysisTask();
        task.setUserId(userId);
        task.setProjectId(projectId);
        task.setTaskType(FULL_ANALYSIS);
        task.setActiveKey(activeKey);
        task.setCreditCost(CreditCostConstants.AI_AUDIT_REPORT);
        task.setStatus("PENDING");
        task.setProgress(0);

        try {
            if (analysisTaskMapper.insert(task) != 1) {
                throw new BusinessException(
                        ErrorCode.OPERATION_ERROR,
                        "审计任务创建失败，请稍后重试"
                );
            }
        } catch (DuplicateKeyException e) {
            AnalysisTask concurrentTask = findActiveTask(userId, projectId, activeKey);
            if (concurrentTask != null) {
                return taskProgressService.getProgress(concurrentTask.getId());
            }

            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR,
                    "审计任务创建失败，请稍后重试"
            );
        }

        taskProgressService.updateProgress(
                task.getId(),
                "PENDING",
                0,
                "任务已创建，等待后台分析",
                null,
                null,
                false
        );

        try {
            asyncExecutor.executeAnalysisTask(task.getId(), projectId, userId);
        } catch (TaskRejectedException e) {
            taskProgressService.updateProgress(
                    task.getId(),
                    "FAILED",
                    100,
                    "当前审计任务较多，请稍后重试",
                    null,
                    "本地审计执行队列已满",
                    true
            );
            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR,
                    "当前审计任务较多，请稍后重试"
            );
        }

        return taskProgressService.getProgress(task.getId());
    }

    public AnalysisTaskVO getTask(Long taskId) {
        Long userId = getCurrentUserId();

        AnalysisTask task = analysisTaskMapper.selectOne(
                new LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getId, taskId)
                        .eq(AnalysisTask::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在或无权限访问");
        }

        AnalysisTaskVO progress = taskProgressService.getProgress(taskId);

        if (progress == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }

        return progress;
    }

    private void checkProjectOwner(Long projectId, Long userId) {
        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, projectId)
                        .eq(Project::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权限分析");
        }
    }

    private AnalysisTask findActiveTask(Long userId, Long projectId, String activeKey) {
        return analysisTaskMapper.selectOne(
                new LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getUserId, userId)
                        .eq(AnalysisTask::getProjectId, projectId)
                        .eq(AnalysisTask::getActiveKey, activeKey)
                        .in(AnalysisTask::getStatus, "PENDING", "RUNNING")
                        .orderByDesc(AnalysisTask::getId)
                        .last("LIMIT 1")
        );
    }

    private String buildActiveKey(Long projectId) {
        return ACTIVE_KEY_PREFIX + projectId;
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userId;
    }
}

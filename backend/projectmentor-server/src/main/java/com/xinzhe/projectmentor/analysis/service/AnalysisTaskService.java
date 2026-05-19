package com.xinzhe.projectmentor.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.analysis.vo.AnalysisTaskVO;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisTaskService {

    private final AnalysisTaskMapper analysisTaskMapper;

    private final ProjectMapper projectMapper;

    private final AnalysisTaskAsyncExecutor asyncExecutor;

    private final TaskProgressService taskProgressService;

    public AnalysisTaskVO startAnalysis(Long projectId) {
        Long userId = getCurrentUserId();

        checkProjectOwner(projectId, userId);

        AnalysisTask task = new AnalysisTask();
        task.setUserId(userId);
        task.setProjectId(projectId);
        task.setTaskType("FULL_ANALYSIS");
        task.setCreditCost(1);
        task.setStatus("PENDING");
        task.setProgress(0);

        analysisTaskMapper.insert(task);

        taskProgressService.updateProgress(
                task.getId(),
                "PENDING",
                0,
                "任务已创建，等待后台分析",
                null,
                null,
                false
        );

        asyncExecutor.executeAnalysisTask(task.getId(), projectId, userId);

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

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userId;
    }
}
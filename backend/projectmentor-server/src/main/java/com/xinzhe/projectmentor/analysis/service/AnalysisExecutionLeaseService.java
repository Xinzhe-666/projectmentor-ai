package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.analysis.message.AnalysisTaskMessage;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AnalysisExecutionLeaseService {

    private final AnalysisTaskMapper taskMapper;
    private final AnalysisPipelineProperties properties;

    public AnalysisClaimResult claim(AnalysisTaskMessage message, String workerId) {
        int claimed = taskMapper.claimExecution(
                message.taskId(),
                message.userId(),
                message.projectId(),
                message.correlationId(),
                workerId,
                message.messageId(),
                properties.getRabbit().getExecution().getLeaseSeconds(),
                properties.getRabbit().getExecution().getMaximumAttempts()
        );

        AnalysisTask task = taskMapper.selectById(message.taskId());
        if (claimed == 1 && task != null) {
            return new AnalysisClaimResult(
                    AnalysisClaimResult.Decision.CLAIMED,
                    toContext(task, message.messageId()),
                    task
            );
        }
        if (task == null
                || !Objects.equals(task.getUserId(), message.userId())
                || !Objects.equals(task.getProjectId(), message.projectId())
                || !Objects.equals(task.getCorrelationId(), message.correlationId())) {
            return new AnalysisClaimResult(AnalysisClaimResult.Decision.INVALID, null, task);
        }
        if ("SUCCESS".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
            return new AnalysisClaimResult(AnalysisClaimResult.Decision.TERMINAL, null, task);
        }
        if ("RUNNING".equals(task.getStatus())) {
            return new AnalysisClaimResult(AnalysisClaimResult.Decision.DUPLICATE_RUNNING, null, task);
        }
        int maximumAttempts = properties.getRabbit().getExecution().getMaximumAttempts();
        if (task.getExecutionAttempt() != null && task.getExecutionAttempt() >= maximumAttempts) {
            return new AnalysisClaimResult(AnalysisClaimResult.Decision.EXHAUSTED, null, task);
        }
        return new AnalysisClaimResult(AnalysisClaimResult.Decision.INVALID, null, task);
    }

    public boolean heartbeat(AnalysisExecutionContext execution) {
        return taskMapper.renewLease(
                execution.taskId(),
                execution.workerId(),
                execution.executionVersion(),
                properties.getRabbit().getExecution().getLeaseSeconds()
        ) == 1;
    }

    public void updateProgress(AnalysisExecutionContext execution, int progress) {
        if (taskMapper.updateFencedProgress(
                execution.taskId(), execution.workerId(), execution.executionVersion(), progress
        ) != 1) {
            throw new StaleAnalysisExecutionException();
        }
    }

    private AnalysisExecutionContext toContext(AnalysisTask task, String messageId) {
        return new AnalysisExecutionContext(
                task.getId(),
                task.getProjectId(),
                task.getUserId(),
                task.getWorkerId(),
                task.getExecutionVersion(),
                task.getExecutionAttempt(),
                messageId,
                task.getCorrelationId()
        );
    }
}

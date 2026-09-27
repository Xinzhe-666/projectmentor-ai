package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisOutboxEvent;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisOutboxMapper;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.analysis.message.AnalysisTaskMessage;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.credit.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AnalysisExecutionTransitionService {

    private final AnalysisTaskMapper taskMapper;
    private final AnalysisOutboxMapper outboxMapper;
    private final AnalysisOutboxFactory outboxFactory;
    private final AnalysisPipelineProperties properties;
    private final CreditService creditService;

    @Transactional(rollbackFor = Exception.class)
    public boolean scheduleRetry(AnalysisExecutionContext execution, String reason) {
        if (taskMapper.releaseForRetry(
                execution.taskId(), execution.workerId(), execution.executionVersion(), SafePipelineError.sanitize(reason)
        ) != 1) {
            return false;
        }
        AnalysisTask task = taskMapper.selectById(execution.taskId());
        int nextAttempt = execution.executionAttempt() + 1;
        long ttlSeconds = retryTtlFor(execution.executionAttempt());
        insertOutbox(outboxFactory.retry(task, nextAttempt, ttlSeconds));
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean failOwnedExecution(AnalysisExecutionContext execution, String reason) {
        if (taskMapper.failOwnedExecution(
                execution.taskId(), execution.workerId(), execution.executionVersion(), SafePipelineError.sanitize(reason)
        ) != 1) {
            return false;
        }
        AnalysisTask terminal = taskMapper.selectById(execution.taskId());
        if (properties.isRabbitMode()) {
            insertOutbox(outboxFactory.dead(terminal));
        }
        refundIfDebited(terminal, "异步分析任务失败幂等退款");
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public AnalysisTask failUnclaimedMessage(AnalysisTaskMessage message, String reason) {
        AnalysisTask task = taskMapper.selectById(message.taskId());
        if (task == null
                || !Objects.equals(task.getUserId(), message.userId())
                || !Objects.equals(task.getProjectId(), message.projectId())
                || !Objects.equals(task.getCorrelationId(), message.correlationId())) {
            return null;
        }
        if ("FAILED".equals(task.getStatus()) || "SUCCESS".equals(task.getStatus())) {
            return task;
        }
        if (taskMapper.failPendingTask(task.getId(), SafePipelineError.sanitize(reason)) != 1) {
            return null;
        }
        AnalysisTask terminal = taskMapper.selectById(task.getId());
        refundIfDebited(terminal, "无效分析消息终结任务幂等退款");
        return terminal;
    }

    @Transactional(rollbackFor = Exception.class)
    public AnalysisTask failClaimedOutboxAndPendingTask(Long outboxId,
                                                        String claimOwner,
                                                        Long taskId,
                                                        String outboxReason,
                                                        String taskReason) {
        if (outboxMapper.markFailed(outboxId, claimOwner, SafePipelineError.sanitize(outboxReason)) != 1) {
            return null;
        }
        AnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return null;
        }
        if ("PENDING".equals(task.getStatus())) {
            if (taskMapper.failPendingTask(taskId, SafePipelineError.sanitize(taskReason)) != 1) {
                throw new IllegalStateException("Failed to terminate task after outbox exhaustion");
            }
            task = taskMapper.selectById(taskId);
        }
        if ("FAILED".equals(task.getStatus())) {
            refundIfDebited(task, "Outbox 发布失败幂等退款");
        }
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public RecoveryResult recoverExpired() {
        List<AnalysisTask> expired = taskMapper.selectExpiredExecutionsForUpdate(
                properties.getRabbit().getExecution().getRecoveryBatchSize()
        );
        List<AnalysisTask> recovered = new ArrayList<>();
        List<AnalysisTask> failed = new ArrayList<>();
        int maximumAttempts = properties.getRabbit().getExecution().getMaximumAttempts();

        for (AnalysisTask task : expired) {
            if (task.getExecutionAttempt() != null && task.getExecutionAttempt() >= maximumAttempts) {
                if (taskMapper.failExpiredExecution(
                        task.getId(), task.getExecutionVersion(), "执行租约过期且已达到最大尝试次数"
                ) == 1) {
                    AnalysisTask terminal = taskMapper.selectById(task.getId());
                    insertOutbox(outboxFactory.dead(terminal));
                    refundIfDebited(terminal, "执行租约超限失败幂等退款");
                    failed.add(terminal);
                }
            } else if (taskMapper.recoverExpiredForRetry(
                    task.getId(), task.getExecutionVersion(), "执行租约过期，任务已恢复"
            ) == 1) {
                AnalysisTask pending = taskMapper.selectById(task.getId());
                long ttlSeconds = retryTtlFor(Math.max(1, task.getExecutionAttempt()));
                insertOutbox(outboxFactory.retry(pending, task.getExecutionAttempt() + 1, ttlSeconds));
                recovered.add(pending);
            }
        }
        return new RecoveryResult(recovered, failed);
    }

    private long retryTtlFor(int completedAttempt) {
        List<Long> ttl = properties.getRabbit().getRetry().getTtlSeconds();
        int index = Math.min(Math.max(0, completedAttempt - 1), ttl.size() - 1);
        return ttl.get(index);
    }

    private void insertOutbox(AnalysisOutboxEvent event) {
        if (outboxMapper.insert(event) != 1) {
            throw new IllegalStateException("Failed to persist analysis outbox event");
        }
    }

    private void refundIfDebited(AnalysisTask task, String remark) {
        if (task == null || task.getId() == null || task.getUserId() == null) {
            return;
        }
        creditService.refundCreditsOnceForTask(
                task.getUserId(),
                CreditCostConstants.OP_AI_AUDIT_REPORT_REFUND,
                task.getId(),
                remark
        );
    }

    public record RecoveryResult(List<AnalysisTask> recovered, List<AnalysisTask> failed) {
    }
}

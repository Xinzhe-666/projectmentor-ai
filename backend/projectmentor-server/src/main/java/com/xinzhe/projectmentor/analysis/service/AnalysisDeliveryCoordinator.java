package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.message.AnalysisTaskMessage;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.credit.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisDeliveryCoordinator {

    public enum DeliveryResult {
        ACK,
        DEAD_LETTER
    }

    private final AnalysisExecutionLeaseService leaseService;
    private final AnalysisTaskProcessor processor;
    private final AnalysisExecutionTransitionService transitions;
    private final CreditService creditService;
    private final AnalysisInstanceIdentity identity;
    private final AnalysisPipelineProperties properties;
    private final AnalysisPipelineMetrics metrics;

    public DeliveryResult handle(AnalysisTaskMessage message, boolean rabbitRetryEnabled) {
        if (message == null || !message.hasRequiredFields()) {
            metrics.message("invalid");
            return DeliveryResult.DEAD_LETTER;
        }
        if (message.schemaVersion() != AnalysisTaskMessage.CURRENT_SCHEMA_VERSION) {
            AnalysisTask failed = transitions.failUnclaimedMessage(message, "不支持的消息协议版本");
            ensureRefund(failed);
            metrics.message("dead-lettered");
            return DeliveryResult.DEAD_LETTER;
        }

        AnalysisClaimResult claim = leaseService.claim(message, identity.newWorkerId());
        switch (claim.decision()) {
            case TERMINAL -> {
                if (claim.task() != null && "FAILED".equals(claim.task().getStatus())) {
                    ensureRefund(claim.task());
                }
                metrics.message("duplicate");
                return DeliveryResult.ACK;
            }
            case DUPLICATE_RUNNING -> {
                metrics.message("duplicate");
                return DeliveryResult.ACK;
            }
            case EXHAUSTED -> {
                AnalysisTask failed = transitions.failUnclaimedMessage(message, "任务已达到最大执行次数");
                ensureRefund(failed);
                metrics.message("dead-lettered");
                return DeliveryResult.DEAD_LETTER;
            }
            case INVALID -> {
                metrics.message("invalid");
                return DeliveryResult.DEAD_LETTER;
            }
            case CLAIMED -> {
                metrics.message("consumed");
            }
        }

        AnalysisExecutionContext execution = claim.execution();
        AnalysisProcessingResult processing = processor.process(execution);
        if (processing.status() == AnalysisProcessingResult.Status.SUCCESS
                || processing.status() == AnalysisProcessingResult.Status.STALE) {
            return DeliveryResult.ACK;
        }

        int maximumAttempts = properties.getRabbit().getExecution().getMaximumAttempts();
        boolean mayRetry = rabbitRetryEnabled
                && processing.status() == AnalysisProcessingResult.Status.RETRYABLE_FAILURE
                && execution.executionAttempt() < maximumAttempts;
        if (mayRetry) {
            if (transitions.scheduleRetry(execution, processing.reason())) {
                metrics.message("retried");
            }
            return DeliveryResult.ACK;
        }

        if (transitions.failOwnedExecution(execution, processing.reason())) {
            AnalysisTask failed = claim.task();
            failed.setStatus("FAILED");
            ensureRefund(failed);
            metrics.message("dead-lettered");
        }
        return DeliveryResult.ACK;
    }

    private void ensureRefund(AnalysisTask task) {
        if (task == null || task.getId() == null || task.getUserId() == null) {
            return;
        }
        creditService.refundCreditsOnceForTask(
                task.getUserId(),
                CreditCostConstants.OP_AI_AUDIT_REPORT_REFUND,
                task.getId(),
                "异步分析任务失败幂等退款"
        );
    }
}

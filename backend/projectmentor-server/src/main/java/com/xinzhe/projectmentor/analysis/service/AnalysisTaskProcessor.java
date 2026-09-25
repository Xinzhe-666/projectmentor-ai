package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisTaskProcessor {

    private final AnalysisReportService reportService;
    private final AnalysisExecutionLeaseService leaseService;
    private final AnalysisFailureClassifier failureClassifier;
    private final AnalysisPipelineMetrics metrics;
    @Qualifier("analysisLeaseTaskScheduler")
    private final TaskScheduler leaseTaskScheduler;
    private final com.xinzhe.projectmentor.config.AnalysisPipelineProperties properties;

    public AnalysisProcessingResult process(AnalysisExecutionContext execution) {
        Timer.Sample timer = metrics.startExecution();
        AtomicBoolean leaseValid = new AtomicBoolean(true);
        ScheduledFuture<?> heartbeat = leaseTaskScheduler.scheduleAtFixedRate(
                () -> renewLease(execution, leaseValid),
                Duration.ofSeconds(properties.getRabbit().getExecution().getHeartbeatSeconds())
        );

        try {
            UserContext.setUserId(execution.userId());
            leaseService.updateProgress(execution, 10);
            leaseService.updateProgress(execution, 35);
            reportService.generateReportForTask(execution);
            log.info(
                    "analysis_execution_success taskId={} messageId={} correlationId={} attempt={} version={}",
                    execution.taskId(), execution.messageId(), execution.correlationId(),
                    execution.executionAttempt(), execution.executionVersion()
            );
            return AnalysisProcessingResult.success();
        } catch (StaleAnalysisExecutionException e) {
            log.warn(
                    "analysis_execution_stale taskId={} messageId={} correlationId={} version={}",
                    execution.taskId(), execution.messageId(), execution.correlationId(), execution.executionVersion()
            );
            return new AnalysisProcessingResult(AnalysisProcessingResult.Status.STALE, SafePipelineError.from(e));
        } catch (Exception e) {
            String reason = SafePipelineError.from(e);
            AnalysisFailureClassifier.FailureType type = failureClassifier.classify(e);
            log.warn(
                    "analysis_execution_failed taskId={} messageId={} correlationId={} attempt={} type={} reason={}",
                    execution.taskId(), execution.messageId(), execution.correlationId(),
                    execution.executionAttempt(), type, reason
            );
            return new AnalysisProcessingResult(
                    type == AnalysisFailureClassifier.FailureType.RETRYABLE
                            ? AnalysisProcessingResult.Status.RETRYABLE_FAILURE
                            : AnalysisProcessingResult.Status.NON_RETRYABLE_FAILURE,
                    reason
            );
        } finally {
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
            UserContext.clear();
            metrics.stopExecution(timer);
        }
    }

    private void renewLease(AnalysisExecutionContext execution, AtomicBoolean leaseValid) {
        if (!leaseValid.get()) {
            return;
        }
        try {
            if (!leaseService.heartbeat(execution)) {
                leaseValid.set(false);
                metrics.lease("lost");
                log.warn(
                        "analysis_lease_lost taskId={} messageId={} correlationId={} version={}",
                        execution.taskId(), execution.messageId(), execution.correlationId(), execution.executionVersion()
                );
            }
        } catch (Exception e) {
            log.warn(
                    "analysis_heartbeat_error taskId={} messageId={} correlationId={} reason={}",
                    execution.taskId(), execution.messageId(), execution.correlationId(), SafePipelineError.from(e)
            );
        }
    }
}

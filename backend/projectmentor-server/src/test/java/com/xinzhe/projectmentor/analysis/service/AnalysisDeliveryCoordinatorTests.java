package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.message.AnalysisTaskMessage;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import com.xinzhe.projectmentor.credit.service.CreditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisDeliveryCoordinatorTests {

    private AnalysisExecutionLeaseService leaseService;
    private AnalysisTaskProcessor processor;
    private AnalysisExecutionTransitionService transitions;
    private CreditService creditService;
    private AnalysisPipelineMetrics metrics;
    private AnalysisPipelineProperties properties;
    private AnalysisDeliveryCoordinator coordinator;

    @BeforeEach
    void setUp() {
        leaseService = mock(AnalysisExecutionLeaseService.class);
        processor = mock(AnalysisTaskProcessor.class);
        transitions = mock(AnalysisExecutionTransitionService.class);
        creditService = mock(CreditService.class);
        metrics = mock(AnalysisPipelineMetrics.class);
        properties = new AnalysisPipelineProperties();
        AnalysisInstanceIdentity identity = mock(AnalysisInstanceIdentity.class);
        when(identity.newWorkerId()).thenReturn("worker-1");
        coordinator = new AnalysisDeliveryCoordinator(
                leaseService, processor, transitions, creditService, identity, properties, metrics
        );
    }

    @Test
    void terminalAndCurrentlyRunningDuplicatesAreAckedWithoutExecution() {
        AnalysisTask terminal = task("SUCCESS", 1);
        when(leaseService.claim(any(), anyString())).thenReturn(
                new AnalysisClaimResult(AnalysisClaimResult.Decision.TERMINAL, null, terminal),
                new AnalysisClaimResult(AnalysisClaimResult.Decision.DUPLICATE_RUNNING, null, task("RUNNING", 1))
        );

        assertThat(coordinator.handle(message(), true)).isEqualTo(AnalysisDeliveryCoordinator.DeliveryResult.ACK);
        assertThat(coordinator.handle(message(), true)).isEqualTo(AnalysisDeliveryCoordinator.DeliveryResult.ACK);
        verify(processor, never()).process(any());
        verify(transitions, never()).scheduleRetry(any(), anyString());
    }

    @Test
    void retryableFailurePersistsRetryBeforeCurrentDeliveryIsAcked() {
        AnalysisExecutionContext execution = execution(1);
        when(leaseService.claim(any(), anyString())).thenReturn(
                new AnalysisClaimResult(AnalysisClaimResult.Decision.CLAIMED, execution, task("RUNNING", 1))
        );
        when(processor.process(execution)).thenReturn(
                new AnalysisProcessingResult(AnalysisProcessingResult.Status.RETRYABLE_FAILURE, "temporary")
        );
        when(transitions.scheduleRetry(execution, "temporary")).thenReturn(true);

        assertThat(coordinator.handle(message(), true)).isEqualTo(AnalysisDeliveryCoordinator.DeliveryResult.ACK);
        verify(transitions).scheduleRetry(execution, "temporary");
        verify(transitions, never()).failOwnedExecution(any(), anyString());
    }

    @Test
    void maximumAttemptBecomesTerminalAndRefundIsTaskIdempotent() {
        properties.getRabbit().getExecution().setMaximumAttempts(4);
        AnalysisExecutionContext execution = execution(4);
        AnalysisTask task = task("RUNNING", 4);
        when(leaseService.claim(any(), anyString())).thenReturn(
                new AnalysisClaimResult(AnalysisClaimResult.Decision.CLAIMED, execution, task)
        );
        when(processor.process(execution)).thenReturn(
                new AnalysisProcessingResult(AnalysisProcessingResult.Status.RETRYABLE_FAILURE, "still unavailable")
        );
        when(transitions.failOwnedExecution(execution, "still unavailable")).thenReturn(true);

        assertThat(coordinator.handle(message(), true)).isEqualTo(AnalysisDeliveryCoordinator.DeliveryResult.ACK);
        verify(transitions, never()).scheduleRetry(any(), anyString());
        verify(transitions).failOwnedExecution(execution, "still unavailable");
        verify(creditService).refundCreditsOnceForTask(
                org.mockito.ArgumentMatchers.eq(7L), anyString(), org.mockito.ArgumentMatchers.eq(100L), anyString()
        );
    }

    @Test
    void unknownSchemaTakesExplicitDeadLetterPathWithoutExecution() {
        AnalysisTaskMessage unsupported = new AnalysisTaskMessage(
                "message-1", 99, 100L, 42L, 7L, 1, "correlation-1", Instant.now()
        );
        when(transitions.failUnclaimedMessage(unsupported, "不支持的消息协议版本"))
                .thenReturn(task("FAILED", 0));

        assertThat(coordinator.handle(unsupported, true))
                .isEqualTo(AnalysisDeliveryCoordinator.DeliveryResult.DEAD_LETTER);
        verify(leaseService, never()).claim(any(), anyString());
        verify(processor, never()).process(any());
    }

    private AnalysisTaskMessage message() {
        return new AnalysisTaskMessage(
                "message-1", 1, 100L, 42L, 7L, 1, "correlation-1", Instant.now()
        );
    }

    private AnalysisExecutionContext execution(int attempt) {
        return new AnalysisExecutionContext(100L, 42L, 7L, "worker-1", 3L,
                attempt, "message-1", "correlation-1");
    }

    private AnalysisTask task(String status, int attempt) {
        AnalysisTask task = new AnalysisTask();
        task.setId(100L);
        task.setUserId(7L);
        task.setProjectId(42L);
        task.setStatus(status);
        task.setExecutionAttempt(attempt);
        return task;
    }
}

package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisOutboxEvent;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisOutboxMapper;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "projectmentor.analysis", name = "dispatch-mode", havingValue = "rabbit")
public class AnalysisOutboxRelay {

    private final AnalysisOutboxClaimService claimService;
    private final AnalysisOutboxMapper outboxMapper;
    private final AnalysisRabbitPublisher publisher;
    private final AnalysisExecutionTransitionService transitions;
    private final AnalysisInstanceIdentity identity;
    private final AnalysisPipelineProperties properties;
    private final AnalysisPipelineMetrics metrics;

    @Scheduled(
            fixedDelayString = "${projectmentor.analysis.rabbit.outbox.poll-interval-millis:1000}",
            initialDelayString = "${projectmentor.analysis.rabbit.outbox.poll-interval-millis:1000}"
    )
    public void relayOnce() {
        String claimOwner = identity.instanceId() + ":relay";
        List<AnalysisOutboxEvent> events;
        try {
            events = claimService.claimBatch(claimOwner);
        } catch (Exception e) {
            log.warn("analysis_outbox_claim_failed owner={} reason={}", claimOwner, SafePipelineError.from(e));
            return;
        }

        if (!events.isEmpty()) {
            metrics.outbox("claimed");
        }
        for (AnalysisOutboxEvent event : events) {
            publishOutsideTransaction(event, claimOwner);
        }

        try {
            outboxMapper.deletePublishedOlderThan(
                    properties.getRabbit().getOutbox().getPublishedRetentionHours()
            );
        } catch (Exception e) {
            log.warn("analysis_outbox_retention_failed reason={}", SafePipelineError.from(e));
        }
    }

    private void publishOutsideTransaction(AnalysisOutboxEvent event, String claimOwner) {
        int renewed = outboxMapper.renewClaim(
                event.getId(), claimOwner, properties.getRabbit().getOutbox().getClaimLeaseSeconds()
        );
        if (renewed != 1) {
            log.info("analysis_outbox_claim_lost eventId={} taskId={} owner={}",
                    event.getEventId(), event.getTaskId(), claimOwner);
            metrics.outbox("claim-lost");
            return;
        }

        RabbitPublishResult result = publisher.publish(event);
        if (result.reliable()) {
            if (outboxMapper.markPublished(event.getId(), claimOwner) == 1) {
                metrics.outbox("published");
            }
            return;
        }

        String reason = SafePipelineError.sanitize(result.status() + ": " + result.reason());
        metrics.outbox(metricResult(result.status()));
        int maximumAttempts = properties.getRabbit().getOutbox().getMaximumAttempts();
        if (event.getPublishAttempt() >= maximumAttempts) {
            AnalysisTask failedTask = transitions.failClaimedOutboxAndPendingTask(
                    event.getId(),
                    claimOwner,
                    event.getTaskId(),
                    reason,
                    "消息发布超过最大尝试次数"
            );
            if (failedTask != null) {
                metrics.outbox("failed");
            }
            return;
        }

        outboxMapper.reschedule(
                event.getId(),
                claimOwner,
                backoffSeconds(event.getPublishAttempt()),
                reason
        );
    }

    private int backoffSeconds(int attempt) {
        AnalysisPipelineProperties.Outbox outbox = properties.getRabbit().getOutbox();
        long multiplier = 1L << Math.min(20, Math.max(0, attempt - 1));
        long delay = Math.min((long) outbox.getMaximumBackoffSeconds(),
                (long) outbox.getInitialBackoffSeconds() * multiplier);
        return (int) delay;
    }

    private String metricResult(RabbitPublishResult.Status status) {
        return switch (status) {
            case NACK -> "nacked";
            case RETURNED -> "returned";
            case TIMEOUT -> "timeout";
            case ERROR -> "error";
            case ACK -> "published";
        };
    }

}

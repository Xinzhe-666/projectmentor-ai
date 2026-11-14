package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.analysis.service.AnalysisInstanceIdentity;
import com.xinzhe.projectmentor.analysis.service.RabbitPublishResult;
import com.xinzhe.projectmentor.analysis.service.SafePipelineError;
import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexOutboxEvent;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexOutboxMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "projectmentor.vector-index", name = "dispatch-mode", havingValue = "rabbit")
public class VectorIndexOutboxRelay {
    private final VectorIndexOutboxClaimService claimService;
    private final VectorIndexOutboxMapper mapper;
    private final VectorIndexRabbitPublisher publisher;
    private final AnalysisInstanceIdentity identity;
    private final VectorIndexProperties properties;
    private final MeterRegistry metrics;
    private final VectorIndexTerminalTransitionService terminalTransitions;

    @Scheduled(fixedDelayString = "${projectmentor.vector-index.outbox-poll-interval-millis:1000}")
    public void relay() {
        String owner = identity.instanceId() + ":index-relay";
        for (VectorIndexOutboxEvent event : claimService.claim(owner)) {
            RabbitPublishResult result = publisher.publish(event);
            if (result.reliable()) {
                mapper.markPublished(event.getId(), owner);
                metrics.counter("pmai.index.outbox", "result", "published").increment();
            } else if (event.getPublishAttempt() >= properties.getOutboxMaximumAttempts()) {
                terminalTransitions.failOutbox(event.getId(), owner, event.getTaskId(),
                        "OUTBOX_EXHAUSTED: " + result.status());
                metrics.counter("pmai.index.outbox", "result", "exhausted").increment();
            } else {
                int delay = Math.min(300, 5 * (1 << Math.min(6, event.getPublishAttempt() - 1)));
                mapper.reschedule(event.getId(), owner, delay,
                        SafePipelineError.sanitize(result.status() + ": " + result.reason()));
                metrics.counter("pmai.index.outbox", "result", "retry").increment();
            }
        }
    }
}

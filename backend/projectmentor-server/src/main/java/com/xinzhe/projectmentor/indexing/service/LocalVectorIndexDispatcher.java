package com.xinzhe.projectmentor.indexing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.analysis.service.AnalysisInstanceIdentity;
import com.xinzhe.projectmentor.analysis.service.SafePipelineError;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexOutboxEvent;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexOutboxMapper;
import com.xinzhe.projectmentor.indexing.message.VectorIndexMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "projectmentor.vector-index", name = "dispatch-mode", havingValue = "local", matchIfMissing = true)
@ConditionalOnBean(VectorIndexWorker.class)
public class LocalVectorIndexDispatcher {
    private final VectorIndexWorker worker;
    private final VectorIndexDeletionService deletionService;
    private final VectorIndexOutboxMapper outboxMapper;
    private final VectorIndexOutboxClaimService claims;
    private final VectorIndexTerminalTransitionService terminalTransitions;
    private final AnalysisInstanceIdentity identity;
    private final ObjectMapper objectMapper;
    private final com.xinzhe.projectmentor.config.VectorIndexProperties properties;

    @Async("vectorIndexTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(VectorIndexTaskSubmissionService.VectorIndexTaskCreatedEvent event) {
        relay();
    }

    @Scheduled(fixedDelayString = "${projectmentor.vector-index.outbox-poll-interval-millis:1000}")
    public void relay() {
        String owner = identity.instanceId() + ":local-index-relay";
        for (VectorIndexOutboxEvent event : claims.claim(owner)) {
            try {
                VectorIndexMessage message = objectMapper.readValue(event.getPayload(), VectorIndexMessage.class);
                VectorIndexWorker.ProcessingResult result = "PROJECT_DELETE".equals(message.eventType())
                        ? deletionService.process(message) : worker.process(message);
                if (result != VectorIndexWorker.ProcessingResult.RETRY_LATER) {
                    outboxMapper.markPublished(event.getId(), owner);
                } else if (event.getPublishAttempt() >= properties.getOutboxMaximumAttempts()) {
                    terminalTransitions.failOutbox(event.getId(), owner, event.getTaskId(), "LOCAL_RETRY_EXHAUSTED");
                } else {
                    outboxMapper.reschedule(event.getId(), owner, retryDelay(event), "Local worker requested retry");
                }
            } catch (Exception error) {
                if (event.getPublishAttempt() >= properties.getOutboxMaximumAttempts()) {
                    terminalTransitions.failOutbox(event.getId(), owner, event.getTaskId(),
                            "LOCAL_DISPATCH_FAILED: " + SafePipelineError.from(error));
                } else {
                    outboxMapper.reschedule(event.getId(), owner, retryDelay(event),
                            SafePipelineError.sanitize(error.getMessage()));
                }
                log.warn("local_vector_dispatch_failed eventId={} reason={}", event.getEventId(),
                        error.getClass().getSimpleName());
            }
        }
    }

    private int retryDelay(VectorIndexOutboxEvent event) {
        int attempt = event.getPublishAttempt() == null ? 1 : event.getPublishAttempt();
        return Math.min(60, 1 << Math.min(5, Math.max(0, attempt - 1)));
    }
}

package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.indexing.mapper.VectorIndexOutboxMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "projectmentor.vector-index", name = "dispatch-mode", havingValue = "local", matchIfMissing = true)
@ConditionalOnBean(VectorIndexWorker.class)
public class LocalVectorIndexDispatcher {
    private final VectorIndexWorker worker;
    private final VectorIndexOutboxMapper outboxMapper;
    private final com.xinzhe.projectmentor.config.VectorIndexProperties properties;

    @Async("vectorIndexTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(VectorIndexTaskSubmissionService.VectorIndexTaskCreatedEvent event) {
        outboxMapper.markLocalPublished(event.outboxId());
        for (int attempt = 0; attempt < properties.getMaximumAttempts(); attempt++) {
            VectorIndexWorker.ProcessingResult result = worker.process(event.message());
            if (result != VectorIndexWorker.ProcessingResult.RETRY_LATER) return;
            try { Thread.sleep(Math.min(2000L, 250L * (attempt + 1))); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
        }
    }
}

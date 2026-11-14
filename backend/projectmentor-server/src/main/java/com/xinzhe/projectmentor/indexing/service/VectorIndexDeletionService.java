package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import com.xinzhe.projectmentor.retrieval.milvus.MilvusChunkStore;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorIndexDeletionService {
    private final ProjectVectorIndexMapper indexMapper;
    private final VectorIndexTaskMapper taskMapper;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<MilvusChunkStore> storeProvider;
    private final MeterRegistry metrics;

    public void request(long userId, long projectId) {
        taskMapper.cancelActiveByProject(projectId, userId);
        indexMapper.markDeleting(projectId, userId);
        events.publishEvent(new ProjectVectorDeleteEvent(userId, projectId));
    }

    @Async("vectorIndexTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void cleanup(ProjectVectorDeleteEvent event) {
        MilvusChunkStore store = storeProvider.getIfAvailable();
        if (store == null) return;
        try {
            store.deleteProject(event.userId(), event.projectId());
            metrics.counter("pmai.index.stale.cleanup", "result", "project-deleted").increment();
        } catch (Exception e) {
            metrics.counter("pmai.index.stale.cleanup", "result", "project-delete-failed").increment();
            log.warn("vector_project_cleanup_failed reason={}", e.getClass().getSimpleName());
        }
    }
    public record ProjectVectorDeleteEvent(long userId, long projectId) {}
}

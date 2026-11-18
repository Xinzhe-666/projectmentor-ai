package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.analysis.service.AnalysisInstanceIdentity;
import com.xinzhe.projectmentor.analysis.service.SafePipelineError;
import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexOutboxEvent;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexTask;
import com.xinzhe.projectmentor.indexing.entity.ProjectVectorIndex;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorFileManifestMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexOutboxMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import com.xinzhe.projectmentor.indexing.message.VectorIndexMessage;
import com.xinzhe.projectmentor.retrieval.milvus.MilvusChunkStore;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorIndexDeletionService {
    private final ProjectVectorIndexMapper indexMapper;
    private final VectorIndexTaskMapper taskMapper;
    private final VectorFileManifestMapper manifestMapper;
    private final VectorIndexOutboxMapper outboxMapper;
    private final VectorIndexTaskSubmissionService submissions;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<MilvusChunkStore> storeProvider;
    private final AnalysisInstanceIdentity identity;
    private final VectorIndexProperties properties;
    private final MeterRegistry metrics;
    private final TransactionTemplate transactions;

    public void request(long userId, long projectId) {
        ProjectVectorIndex state = indexMapper.selectOwned(projectId, userId);
        if (state == null) return;
        taskMapper.cancelActiveByProject(projectId, userId);
        indexMapper.markDeleting(projectId, userId);

        VectorIndexTask task = new VectorIndexTask();
        task.setProjectId(projectId);
        task.setUserId(userId);
        task.setTaskType("CLEANUP");
        task.setActiveKey("cleanup:project:" + projectId);
        task.setCorrelationId(UUID.randomUUID().toString());
        task.setGeneration(0L);
        task.setTargetRevision(0L);
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setExecutionAttempt(0);
        task.setExecutionVersion(0L);
        if (taskMapper.insert(task) != 1) throw new IllegalStateException("Failed to create vector cleanup task");

        VectorIndexMessage message = new VectorIndexMessage(UUID.randomUUID().toString(), 1, "PROJECT_DELETE",
                task.getId(), userId, projectId, 0, task.getCorrelationId());
        VectorIndexOutboxEvent outbox = submissions.outbox(task.getId(), message);
        if (outboxMapper.insert(outbox) != 1) throw new IllegalStateException("Failed to persist vector cleanup outbox");
        events.publishEvent(new VectorIndexTaskSubmissionService.VectorIndexTaskCreatedEvent(message, outbox.getId()));
    }

    public VectorIndexWorker.ProcessingResult process(VectorIndexMessage message) {
        String workerId = identity.newWorkerId();
        String buildId = UUID.randomUUID().toString();
        int claimed = taskMapper.claim(message.taskId(), message.userId(), message.projectId(), 0,
                workerId, buildId, message.messageId(), properties.getLeaseSeconds(), properties.getMaximumAttempts());
        VectorIndexTask task = taskMapper.selectById(message.taskId());
        if (claimed != 1) {
            if (task == null || "SUCCEEDED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
                return VectorIndexWorker.ProcessingResult.ACK;
            }
            return VectorIndexWorker.ProcessingResult.RETRY_LATER;
        }
        try {
            MilvusChunkStore store = storeProvider.getIfAvailable();
            if (store == null) throw new IllegalStateException("MILVUS_UNAVAILABLE");
            store.deleteProject(message.userId(), message.projectId());
            transactions.executeWithoutResult(ignored -> completeCleanup(task, workerId, buildId));
            metrics.counter("pmai.index.stale.cleanup", "result", "project-deleted").increment();
            return VectorIndexWorker.ProcessingResult.ACK;
        } catch (Exception error) {
            String safe = SafePipelineError.sanitize(error.getMessage());
            boolean retry = task.getExecutionAttempt() < properties.getMaximumAttempts()
                    && taskMapper.releaseForRetry(task.getId(), workerId, task.getExecutionVersion(), buildId, safe) == 1;
            if (!retry) {
                taskMapper.failOwned(task.getId(), workerId, task.getExecutionVersion(), buildId,
                        "PROJECT_CLEANUP_FAILED", safe);
            }
            metrics.counter("pmai.index.stale.cleanup", "result", "project-delete-failed").increment();
            log.warn("vector_project_cleanup_failed taskId={} reason={}", task.getId(), error.getClass().getSimpleName());
            return retry ? VectorIndexWorker.ProcessingResult.RETRY_LATER : VectorIndexWorker.ProcessingResult.REJECT;
        }
    }

    private void completeCleanup(VectorIndexTask task, String workerId, String buildId) {
        if (taskMapper.complete(task.getId(), workerId, task.getExecutionVersion(), buildId) != 1) {
            throw new StaleVectorIndexExecutionException();
        }
        manifestMapper.deleteProject(task.getProjectId());
        indexMapper.deleteOwned(task.getProjectId(), task.getUserId());
    }
}

package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.analysis.service.AnalysisInstanceIdentity;
import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.indexing.entity.ProjectVectorIndex;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexOutboxEvent;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexTask;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorFileManifestMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexOutboxMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import com.xinzhe.projectmentor.indexing.message.VectorIndexMessage;
import com.xinzhe.projectmentor.retrieval.milvus.MilvusChunkStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VectorIndexDeletionServiceTests {
    private final ProjectVectorIndexMapper indexMapper = mock(ProjectVectorIndexMapper.class);
    private final VectorIndexTaskMapper taskMapper = mock(VectorIndexTaskMapper.class);
    private final VectorFileManifestMapper manifestMapper = mock(VectorFileManifestMapper.class);
    private final VectorIndexOutboxMapper outboxMapper = mock(VectorIndexOutboxMapper.class);
    private final VectorIndexTaskSubmissionService submissions = mock(VectorIndexTaskSubmissionService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<MilvusChunkStore> stores = mock(ObjectProvider.class);
    private final AnalysisInstanceIdentity identity = mock(AnalysisInstanceIdentity.class);
    private final VectorIndexProperties properties = new VectorIndexProperties();
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private final VectorIndexDeletionService service = new VectorIndexDeletionService(indexMapper, taskMapper,
            manifestMapper, outboxMapper, submissions, events, stores, identity, properties,
            new SimpleMeterRegistry(), transactions);

    @BeforeEach
    void executeTransactionCallbacks() {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked") Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
    }

    @Test
    void deletionRequestPersistsCleanupTaskAndOutboxBeforeDispatch() {
        ProjectVectorIndex state = new ProjectVectorIndex();
        when(indexMapper.selectOwned(31, 21)).thenReturn(state);
        doAnswer(invocation -> {
            VectorIndexTask task = invocation.getArgument(0);
            task.setId(77L);
            return 1;
        }).when(taskMapper).insert(any(VectorIndexTask.class));
        VectorIndexOutboxEvent outbox = new VectorIndexOutboxEvent();
        outbox.setId(88L);
        when(submissions.outbox(eq(77L), any(VectorIndexMessage.class))).thenReturn(outbox);
        when(outboxMapper.insert(outbox)).thenReturn(1);

        service.request(21, 31);

        verify(taskMapper).cancelActiveByProject(31, 21);
        verify(indexMapper).markDeleting(31, 21);
        verify(submissions).outbox(eq(77L), argThat(message -> "PROJECT_DELETE".equals(message.eventType())));
        verify(outboxMapper).insert(outbox);
        verify(events).publishEvent(any(VectorIndexTaskSubmissionService.VectorIndexTaskCreatedEvent.class));
    }

    @Test
    void replayAfterCleanupCompletionIsIdempotent() {
        MilvusChunkStore store = mock(MilvusChunkStore.class);
        when(stores.getIfAvailable()).thenReturn(store);
        when(identity.newWorkerId()).thenReturn("worker-a", "worker-b");
        when(taskMapper.claim(eq(77L), eq(21L), eq(31L), eq(0L), anyString(), anyString(),
                eq("message-1"), anyInt(), anyInt())).thenReturn(1, 0);
        VectorIndexTask running = new VectorIndexTask();
        running.setId(77L); running.setProjectId(31L); running.setUserId(21L);
        running.setStatus("RUNNING"); running.setExecutionVersion(1L); running.setExecutionAttempt(1);
        VectorIndexTask succeeded = new VectorIndexTask();
        succeeded.setId(77L); succeeded.setStatus("SUCCEEDED");
        when(taskMapper.selectById(77L)).thenReturn(running, succeeded);
        when(taskMapper.complete(eq(77L), eq("worker-a"), eq(1L), anyString())).thenReturn(1);
        VectorIndexMessage message = new VectorIndexMessage("message-1", 1, "PROJECT_DELETE",
                77, 21, 31, 0, "correlation-1");

        assertEquals(VectorIndexWorker.ProcessingResult.ACK, service.process(message));
        assertEquals(VectorIndexWorker.ProcessingResult.ACK, service.process(message));

        verify(store, times(1)).deleteProject(21, 31);
        verify(manifestMapper, times(1)).deleteProject(31);
        verify(indexMapper, times(1)).deleteOwned(31, 21);
    }
}

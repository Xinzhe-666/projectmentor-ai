package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.analysis.service.AnalysisInstanceIdentity;
import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexTask;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import com.xinzhe.projectmentor.indexing.message.VectorIndexMessage;
import com.xinzhe.projectmentor.retrieval.chunk.CodeChunk;
import com.xinzhe.projectmentor.retrieval.chunk.EvidenceAwareCodeChunker;
import com.xinzhe.projectmentor.retrieval.chunk.ProjectContentHasher;
import com.xinzhe.projectmentor.retrieval.embedding.EmbeddingProvider;
import com.xinzhe.projectmentor.retrieval.milvus.MilvusChunkStore;
import com.xinzhe.projectmentor.retrieval.milvus.MilvusCollectionManager;
import com.xinzhe.projectmentor.retrieval.search.RetrievalCandidate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VectorIndexWorkerFencingTests {
    @Test
    void leaseTakeoverKeepsWinningBuildSearchableAndOldCleanupIsBuildScoped() throws Exception {
        VectorIndexTaskMapper tasks = mock(VectorIndexTaskMapper.class);
        ProjectVectorIndexMapper indexes = mock(ProjectVectorIndexMapper.class);
        ProjectFileMapper files = mock(ProjectFileMapper.class);
        EvidenceAwareCodeChunker chunker = mock(EvidenceAwareCodeChunker.class);
        EmbeddingProvider embedding = mock(EmbeddingProvider.class);
        MilvusCollectionManager collections = mock(MilvusCollectionManager.class);
        MilvusChunkStore store = mock(MilvusChunkStore.class);
        VectorGenerationCommitService commits = mock(VectorGenerationCommitService.class);
        AnalysisInstanceIdentity identity = mock(AnalysisInstanceIdentity.class);
        ThreadPoolTaskScheduler scheduler = mock(ThreadPoolTaskScheduler.class);
        @SuppressWarnings("unchecked") ScheduledFuture<Object> heartbeatFuture = mock(ScheduledFuture.class);
        doReturn(heartbeatFuture).when(scheduler)
                .scheduleAtFixedRate(any(Runnable.class), any(java.time.Duration.class));
        when(identity.newWorkerId()).thenReturn("worker-a", "worker-b");

        ProjectContentHasher hasher = new ProjectContentHasher();
        ProjectFile file = new ProjectFile();
        file.setId(101L); file.setProjectId(31L); file.setFilePath("src/Auth.java");
        file.setContent("canonical jwt implementation");
        CodeChunk chunk = new CodeChunk("logical-chunk", 101L, file.getFilePath(), "java", "CODE",
                "Auth", 1, 1, hasher.sha256(file.getContent()), file.getContent(), "jwt", "jwt");
        when(files.selectList(any())).thenReturn(List.of(file));
        when(chunker.chunk(31, file, "embedding-model")).thenReturn(List.of(chunk));
        when(embedding.model()).thenReturn("embedding-model");
        when(embedding.embed(anyList())).thenReturn(List.of(new float[]{1, 0, 0, 0}));
        when(indexes.markRunningFenced(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
                anyString(), anyLong(), anyString())).thenReturn(1);
        when(store.hybridSearch(anyLong(), anyLong(), anyLong(), anyString(), anyString(), any(float[].class)))
                .thenReturn(List.of(candidate(chunk)));

        AtomicReference<String> buildA = new AtomicReference<>();
        AtomicReference<String> buildB = new AtomicReference<>();
        AtomicLong version = new AtomicLong();
        ThreadLocal<VectorIndexTask> claimedTask = new ThreadLocal<>();
        when(tasks.claim(anyLong(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString(),
                anyInt(), anyInt())).thenAnswer(invocation -> {
            String worker = invocation.getArgument(4);
            String build = invocation.getArgument(5);
            if ("worker-a".equals(worker)) buildA.set(build); else buildB.set(build);
            VectorIndexTask task = new VectorIndexTask();
            task.setId(77L); task.setUserId(21L); task.setProjectId(31L); task.setGeneration(4L);
            task.setTargetRevision(9L); task.setBuildId(build); task.setStatus("RUNNING");
            task.setExecutionVersion(version.incrementAndGet()); task.setExecutionAttempt((int) version.get());
            claimedTask.set(task);
            return 1;
        });
        when(tasks.selectById(77L)).thenAnswer(ignored -> claimedTask.get());

        CountDownLatch aInserted = new CountDownLatch(1);
        CountDownLatch bCompleted = new CountDownLatch(1);
        when(store.insert(eq(21L), eq(31L), eq(4L), anyString(), anyList(), anyList()))
                .thenAnswer(invocation -> {
                    String build = invocation.getArgument(3);
                    if (build.equals(buildA.get())) {
                        aInserted.countDown();
                        assertTrue(bCompleted.await(5, TimeUnit.SECONDS));
                    }
                    return 1L;
                });
        when(tasks.heartbeat(anyLong(), anyString(), anyLong(), anyString(), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    String worker = invocation.getArgument(1);
                    return "worker-a".equals(worker) && bCompleted.getCount() == 0 ? 0 : 1;
                });
        when(commits.commit(any(), anyList(), eq(1))).thenAnswer(invocation -> {
            VectorIndexExecutionContext execution = invocation.getArgument(0);
            assertEquals(buildB.get(), execution.buildId());
            return new VectorGenerationCommitService.CommitResult(true, null, null);
        });

        VectorIndexProperties properties = new VectorIndexProperties();
        properties.setMaximumAttempts(3); properties.setLeaseSeconds(30); properties.setHeartbeatSeconds(10);
        VectorIndexWorker worker = new VectorIndexWorker(tasks, indexes, files, chunker, hasher, embedding,
                collections, store, commits, properties, identity, scheduler, new SimpleMeterRegistry());
        VectorIndexMessage message = new VectorIndexMessage("message-1", 1, "INDEX_EXECUTE",
                77, 21, 31, 4, "correlation-1");

        CompletableFuture<VectorIndexWorker.ProcessingResult> attemptA =
                CompletableFuture.supplyAsync(() -> worker.process(message));
        assertTrue(aInserted.await(5, TimeUnit.SECONDS));
        VectorIndexWorker.ProcessingResult resultB = worker.process(message);
        bCompleted.countDown();
        VectorIndexWorker.ProcessingResult resultA = attemptA.get(5, TimeUnit.SECONDS);

        assertEquals(VectorIndexWorker.ProcessingResult.ACK, resultB);
        assertEquals(VectorIndexWorker.ProcessingResult.REJECT, resultA);
        assertNotNull(buildA.get()); assertNotNull(buildB.get()); assertNotEquals(buildA.get(), buildB.get());
        verify(store).deleteBuild(21, 31, 4, buildA.get());
        verify(store, never()).deleteBuild(21, 31, 4, buildB.get());
        verify(store).hybridSearch(eq(21L), eq(31L), eq(4L), eq(buildB.get()), anyString(), any(float[].class));
        verify(commits, times(1)).commit(any(), anyList(), eq(1));
    }

    private RetrievalCandidate candidate(CodeChunk chunk) {
        return new RetrievalCandidate(chunk.chunkId(), chunk.fileId(), chunk.filePath(), chunk.language(),
                chunk.chunkType(), chunk.symbolName(), chunk.startLine(), chunk.endLine(), chunk.contentHash(),
                chunk.content(), null, null, null, null, 1, 0.03, 0, 0, List.of(), List.of());
    }
}

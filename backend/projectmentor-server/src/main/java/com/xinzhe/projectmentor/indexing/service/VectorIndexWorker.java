package com.xinzhe.projectmentor.indexing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.analysis.service.AnalysisInstanceIdentity;
import com.xinzhe.projectmentor.analysis.service.SafePipelineError;
import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.indexing.entity.ProjectVectorIndex;
import com.xinzhe.projectmentor.indexing.entity.VectorFileManifest;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexTask;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import com.xinzhe.projectmentor.indexing.message.VectorIndexMessage;
import com.xinzhe.projectmentor.retrieval.chunk.CodeChunk;
import com.xinzhe.projectmentor.retrieval.chunk.EvidenceAwareCodeChunker;
import com.xinzhe.projectmentor.retrieval.chunk.ProjectContentHasher;
import com.xinzhe.projectmentor.retrieval.embedding.EmbeddingException;
import com.xinzhe.projectmentor.retrieval.embedding.EmbeddingProvider;
import com.xinzhe.projectmentor.retrieval.milvus.MilvusChunkStore;
import com.xinzhe.projectmentor.retrieval.milvus.MilvusCollectionManager;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "projectmentor.milvus", name = "enabled", havingValue = "true")
public class VectorIndexWorker {
    private final VectorIndexTaskMapper taskMapper;
    private final ProjectVectorIndexMapper indexMapper;
    private final ProjectFileMapper fileMapper;
    private final EvidenceAwareCodeChunker chunker;
    private final ProjectContentHasher contentHasher;
    private final EmbeddingProvider embedding;
    private final MilvusCollectionManager collectionManager;
    private final MilvusChunkStore chunkStore;
    private final VectorGenerationCommitService commitService;
    private final VectorIndexProperties properties;
    private final AnalysisInstanceIdentity identity;
    private final ThreadPoolTaskScheduler analysisLeaseTaskScheduler;
    private final MeterRegistry metrics;

    public ProcessingResult process(VectorIndexMessage message) {
        String workerId = identity.newWorkerId();
        String buildId = UUID.randomUUID().toString();
        int claimed = taskMapper.claim(message.taskId(), message.userId(), message.projectId(), message.generation(),
                workerId, buildId, message.messageId(), properties.getLeaseSeconds(), properties.getMaximumAttempts());
        VectorIndexTask task = taskMapper.selectById(message.taskId());
        if (claimed != 1) {
            if (task == null || "SUCCEEDED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
                return ProcessingResult.ACK;
            }
            return ProcessingResult.RETRY_LATER;
        }
        VectorIndexExecutionContext execution = new VectorIndexExecutionContext(task.getId(), task.getUserId(),
                task.getProjectId(), task.getGeneration(), task.getTargetRevision(), task.getBuildId(), workerId,
                task.getExecutionVersion(), task.getExecutionAttempt());
        ScheduledFuture<?> heartbeat = startHeartbeat(execution);
        long started = System.nanoTime();
        try {
            requireHeartbeat(execution, 8);
            if (indexMapper.markRunningFenced(execution.projectId(), execution.userId(), execution.generation(),
                    execution.targetRevision(), execution.taskId(), execution.workerId(),
                    execution.executionVersion(), execution.buildId()) != 1) {
                throw new StaleVectorIndexExecutionException();
            }
            collectionManager.ensureCollection();
            List<ProjectFile> files = fileMapper.selectList(new LambdaQueryWrapper<ProjectFile>()
                    .eq(ProjectFile::getProjectId, execution.projectId()).orderByAsc(ProjectFile::getFilePath));
            List<CodeChunk> chunks = new ArrayList<>();
            Map<ProjectFile, List<CodeChunk>> byFile = new LinkedHashMap<>();
            for (ProjectFile file : files) {
                List<CodeChunk> fileChunks = chunker.chunk(execution.projectId(), file, embedding.model());
                if (!fileChunks.isEmpty()) { byFile.put(file, fileChunks); chunks.addAll(fileChunks); }
            }
            requireHeartbeat(execution, 25);
            List<float[]> vectors = embedding.embed(chunks.stream().map(CodeChunk::embeddingText).toList());
            requireHeartbeat(execution, 65);
            long inserted = chunkStore.insert(execution.userId(), execution.projectId(), execution.generation(),
                    execution.buildId(), chunks, vectors);
            requireHeartbeat(execution, 75);
            if (inserted != chunks.size()) throw new IllegalStateException("MILVUS_INSERT_COUNT_MISMATCH");
            if (!chunks.isEmpty() && chunkStore.hybridSearch(execution.userId(), execution.projectId(),
                    execution.generation(), execution.buildId(), chunks.get(0).symbolName(), vectors.get(0)).isEmpty()) {
                throw new IllegalStateException("MILVUS_GENERATION_NOT_SEARCHABLE");
            }
            validateSnapshot(execution.projectId(), byFile);
            requireHeartbeat(execution, 90);
            List<VectorFileManifest> manifests = manifests(execution, byFile);
            VectorGenerationCommitService.CommitResult commit = commitService.commit(execution, manifests, chunks.size());
            if (!commit.activated()) {
                chunkStore.deleteBuild(execution.userId(), execution.projectId(), execution.generation(), execution.buildId());
                metrics.counter("pmai.index.task", "result", "superseded").increment();
                return ProcessingResult.ACK;
            }
            metrics.counter("pmai.index.task", "result", "completed").increment();
            metrics.counter("pmai.index.generation.switch", "result", "success").increment();
            metrics.summary("pmai.index.files").record(manifests.size());
            metrics.summary("pmai.index.chunks").record(chunks.size());
            if (commit.previousGeneration() != null && commit.previousBuildId() != null
                    && commit.previousGeneration() != execution.generation()) {
                try {
                    chunkStore.deleteBuild(execution.userId(), execution.projectId(),
                            commit.previousGeneration(), commit.previousBuildId());
                    metrics.counter("pmai.index.stale.cleanup", "result", "success").increment();
                } catch (RuntimeException cleanupError) {
                    metrics.counter("pmai.index.stale.cleanup", "result", "failed").increment();
                    log.warn("vector_generation_cleanup_failed taskId={} generation={} reason={}",
                            execution.taskId(), commit.previousGeneration(), SafePipelineError.from(cleanupError));
                }
            }
            return ProcessingResult.ACK;
        } catch (Exception error) {
            String code = errorCode(error);
            String safe = SafePipelineError.sanitize(error.getMessage());
            try { chunkStore.deleteBuild(execution.userId(), execution.projectId(), execution.generation(), execution.buildId()); }
            catch (Exception ignored) { metrics.counter("pmai.index.stale.cleanup", "result", "failed").increment(); }
            boolean shouldRetry = retryable(error) && execution.executionAttempt() < properties.getMaximumAttempts();
            if (shouldRetry) {
                if (taskMapper.releaseForRetry(execution.taskId(), execution.workerId(), execution.executionVersion(),
                        execution.buildId(), safe) != 1) {
                    shouldRetry = false;
                }
            }
            if (!shouldRetry) {
                commitService.fail(execution, code, safe);
                metrics.counter("pmai.index.task", "result", "failed").increment();
            }
            log.warn("vector_index_failed taskId={} code={} reason={}", execution.taskId(), code, safe);
            return shouldRetry ? ProcessingResult.RETRY_LATER : ProcessingResult.REJECT;
        } finally {
            heartbeat.cancel(false);
            metrics.timer("pmai.index.duration", "result", "finished")
                    .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
        }
    }

    private ScheduledFuture<?> startHeartbeat(VectorIndexExecutionContext execution) {
        return analysisLeaseTaskScheduler.scheduleAtFixedRate(() -> {
            try { taskMapper.heartbeat(execution.taskId(), execution.workerId(), execution.executionVersion(),
                    execution.buildId(), properties.getLeaseSeconds(), 10); }
            catch (Exception e) { log.warn("vector_index_heartbeat_failed taskId={}", execution.taskId()); }
        }, Duration.ofSeconds(properties.getHeartbeatSeconds()));
    }

    private void requireHeartbeat(VectorIndexExecutionContext execution, int progress) {
        if (taskMapper.heartbeat(execution.taskId(), execution.workerId(), execution.executionVersion(),
                execution.buildId(), properties.getLeaseSeconds(), progress) != 1) {
            throw new StaleVectorIndexExecutionException();
        }
    }

    private List<VectorFileManifest> manifests(VectorIndexExecutionContext execution,
                                                Map<ProjectFile, List<CodeChunk>> byFile) {
        List<VectorFileManifest> result = new ArrayList<>();
        for (Map.Entry<ProjectFile, List<CodeChunk>> entry : byFile.entrySet()) {
            VectorFileManifest manifest = new VectorFileManifest();
            manifest.setProjectId(execution.projectId()); manifest.setUserId(execution.userId());
            manifest.setFileId(entry.getKey().getId()); manifest.setFilePath(entry.getKey().getFilePath());
            manifest.setContentHash(entry.getValue().get(0).contentHash()); manifest.setGeneration(execution.generation());
            manifest.setBuildId(execution.buildId());
            manifest.setEmbeddingModel(embedding.model()); manifest.setChunkCount(entry.getValue().size());
            manifest.setIndexedAt(LocalDateTime.now()); result.add(manifest);
        }
        return result;
    }

    private void validateSnapshot(long projectId, Map<ProjectFile, List<CodeChunk>> snapshot) {
        Map<Long, String> expected = new java.util.HashMap<>();
        for (Map.Entry<ProjectFile, List<CodeChunk>> entry : snapshot.entrySet()) {
            expected.put(entry.getKey().getId(), entry.getValue().get(0).contentHash());
        }
        List<ProjectFile> current = fileMapper.selectList(new LambdaQueryWrapper<ProjectFile>()
                .eq(ProjectFile::getProjectId, projectId).orderByAsc(ProjectFile::getFilePath));
        long presentExpected = current.stream().filter(file -> expected.containsKey(file.getId())).count();
        if (presentExpected != expected.size()) throw new IllegalStateException("INDEX_SOURCE_CHANGED");
        for (ProjectFile file : current) {
            String hash = expected.get(file.getId());
            if (hash == null && !chunker.chunk(projectId, file, embedding.model()).isEmpty()) {
                throw new IllegalStateException("INDEX_SOURCE_CHANGED");
            }
            if (hash != null && !hash.equals(contentHasher.sha256(file.getContent()))) {
                throw new IllegalStateException("INDEX_SOURCE_CHANGED");
            }
        }
    }

    private String errorCode(Exception error) {
        if (error instanceof EmbeddingException ee) return "EMBEDDING_" + ee.code();
        if (error instanceof StaleVectorIndexExecutionException) return "INDEX_LEASE_LOST";
        String message = error.getMessage();
        if ("INDEX_SOURCE_CHANGED".equals(message)) return message;
        if (message != null && message.startsWith("MILVUS_")) return message;
        return "INDEX_EXECUTION_FAILED";
    }
    private boolean retryable(Exception error) {
        return error instanceof EmbeddingException ee ? ee.retryable()
                : !(error instanceof StaleVectorIndexExecutionException);
    }
    public enum ProcessingResult { ACK, RETRY_LATER, REJECT }
}

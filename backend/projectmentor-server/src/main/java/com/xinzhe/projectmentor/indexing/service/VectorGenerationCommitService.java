package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.indexing.entity.VectorFileManifest;
import com.xinzhe.projectmentor.indexing.entity.ProjectVectorIndex;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorFileManifestMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorGenerationCommitService {
    private final ProjectVectorIndexMapper indexMapper;
    private final VectorIndexTaskMapper taskMapper;
    private final VectorFileManifestMapper manifestMapper;
    private final VectorIndexTaskSubmissionService submissionService;

    @Transactional(rollbackFor = Exception.class)
    public CommitResult commit(VectorIndexExecutionContext execution, List<VectorFileManifest> manifests, int chunkCount) {
        var state = indexMapper.selectForUpdate(execution.projectId());
        if (state == null || !java.util.Objects.equals(state.getUserId(), execution.userId())
                || !java.util.Objects.equals(state.getPendingGeneration(), execution.generation())
                || !java.util.Objects.equals(state.getPendingBuildId(), execution.buildId())) {
            throw new StaleVectorIndexExecutionException();
        }
        if (!java.util.Objects.equals(state.getSourceRevision(), execution.targetRevision())) {
            if (taskMapper.supersedeOwned(execution.taskId(), execution.workerId(), execution.executionVersion(),
                    execution.buildId()) != 1) throw new StaleVectorIndexExecutionException();
            submissionService.scheduleLatestRevision(execution.projectId(), execution.userId());
            return new CommitResult(false, state.getActiveGeneration(), state.getActiveBuildId());
        }

        manifestMapper.deleteBuild(execution.projectId(), execution.generation(), execution.buildId());
        for (VectorFileManifest manifest : manifests) {
            if (manifestMapper.insert(manifest) != 1) throw new IllegalStateException("Manifest insert failed");
        }
        if (taskMapper.complete(execution.taskId(), execution.workerId(), execution.executionVersion(),
                execution.buildId()) != 1) {
            throw new StaleVectorIndexExecutionException();
        }
        if (indexMapper.switchGeneration(execution.projectId(), execution.userId(), execution.generation(),
                execution.buildId(), execution.targetRevision(), chunkCount, manifests.size()) != 1) {
            throw new StaleVectorIndexExecutionException();
        }
        return new CommitResult(true, state.getActiveGeneration(), state.getActiveBuildId());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean fail(VectorIndexExecutionContext execution, String code, String message) {
        if (taskMapper.failOwned(execution.taskId(), execution.workerId(), execution.executionVersion(),
                execution.buildId(), code, message) != 1) {
            return false;
        }
        ProjectVectorIndex state = indexMapper.selectForUpdate(execution.projectId());
        if (indexMapper.failPending(execution.projectId(), execution.userId(), execution.generation(),
                execution.buildId(), code, message) != 1) {
            throw new IllegalStateException("Failed to mark vector index generation failed");
        }
        if (state != null && (Boolean.TRUE.equals(state.getRescheduleRequired())
                || !java.util.Objects.equals(state.getSourceRevision(), execution.targetRevision()))) {
            submissionService.scheduleLatestRevision(execution.projectId(), execution.userId());
        }
        return true;
    }

    public record CommitResult(boolean activated, Long previousGeneration, String previousBuildId) {}
}

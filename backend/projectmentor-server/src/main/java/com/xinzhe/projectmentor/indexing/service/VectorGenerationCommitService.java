package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.indexing.entity.VectorFileManifest;
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

    @Transactional(rollbackFor = Exception.class)
    public void commit(VectorIndexExecutionContext execution, List<VectorFileManifest> manifests, int chunkCount) {
        manifestMapper.deleteGeneration(execution.projectId(), execution.generation());
        for (VectorFileManifest manifest : manifests) {
            if (manifestMapper.insert(manifest) != 1) throw new IllegalStateException("Manifest insert failed");
        }
        if (taskMapper.complete(execution.taskId(), execution.workerId(), execution.executionVersion()) != 1) {
            throw new StaleVectorIndexExecutionException();
        }
        if (indexMapper.switchGeneration(execution.projectId(), execution.userId(), execution.generation(),
                chunkCount, manifests.size()) != 1) {
            throw new StaleVectorIndexExecutionException();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean fail(VectorIndexExecutionContext execution, String code, String message) {
        if (taskMapper.failOwned(execution.taskId(), execution.workerId(), execution.executionVersion(), code, message) != 1) {
            return false;
        }
        if (indexMapper.failPending(execution.projectId(), execution.userId(), execution.generation(), code, message) != 1) {
            throw new IllegalStateException("Failed to mark vector index generation failed");
        }
        return true;
    }
}

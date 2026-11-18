package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.indexing.entity.VectorFileManifest;
import com.xinzhe.projectmentor.indexing.entity.ProjectVectorIndex;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorFileManifestMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VectorGenerationCommitServiceTests {
    private final ProjectVectorIndexMapper indexMapper = mock(ProjectVectorIndexMapper.class);
    private final VectorIndexTaskMapper taskMapper = mock(VectorIndexTaskMapper.class);
    private final VectorFileManifestMapper manifestMapper = mock(VectorFileManifestMapper.class);
    private final VectorIndexTaskSubmissionService submissions = mock(VectorIndexTaskSubmissionService.class);
    private final VectorGenerationCommitService service =
            new VectorGenerationCommitService(indexMapper, taskMapper, manifestMapper, submissions);
    private final VectorIndexExecutionContext execution =
            new VectorIndexExecutionContext(11, 22, 33, 2, 5, "build-a", "worker-a", 7, 1);

    private ProjectVectorIndex matchingState() {
        ProjectVectorIndex state = new ProjectVectorIndex();
        state.setUserId(22L);
        state.setPendingGeneration(2L);
        state.setPendingBuildId("build-a");
        state.setSourceRevision(5L);
        state.setActiveGeneration(1L);
        state.setActiveBuildId("build-old");
        return state;
    }

    @Test
    void switchesGenerationOnlyAfterManifestAndFencedTaskCompletion() {
        VectorFileManifest manifest = new VectorFileManifest();
        when(indexMapper.selectForUpdate(33)).thenReturn(matchingState());
        when(manifestMapper.insert(manifest)).thenReturn(1);
        when(taskMapper.complete(11, "worker-a", 7, "build-a")).thenReturn(1);
        when(indexMapper.switchGeneration(33, 22, 2, "build-a", 5, 9, 1)).thenReturn(1);

        service.commit(execution, List.of(manifest), 9);

        var order = inOrder(manifestMapper, taskMapper, indexMapper);
        order.verify(manifestMapper).deleteBuild(33, 2, "build-a");
        order.verify(manifestMapper).insert(manifest);
        order.verify(taskMapper).complete(11, "worker-a", 7, "build-a");
        order.verify(indexMapper).switchGeneration(33, 22, 2, "build-a", 5, 9, 1);
    }

    @Test
    void staleWorkerCannotActivatePendingGeneration() {
        when(indexMapper.selectForUpdate(33)).thenReturn(matchingState());
        when(taskMapper.complete(11, "worker-a", 7, "build-a")).thenReturn(0);

        assertThrows(StaleVectorIndexExecutionException.class,
                () -> service.commit(execution, List.of(), 0));

        verify(indexMapper, never()).switchGeneration(anyLong(), anyLong(), anyLong(), anyString(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void sourceRevisionChangeSupersedesAttemptAndSchedulesLatestRevision() {
        ProjectVectorIndex changed = matchingState();
        changed.setSourceRevision(6L);
        when(indexMapper.selectForUpdate(33)).thenReturn(changed);
        when(taskMapper.supersedeOwned(11, "worker-a", 7, "build-a")).thenReturn(1);

        VectorGenerationCommitService.CommitResult result = service.commit(execution, List.of(), 0);

        assertFalse(result.activated());
        verify(submissions).scheduleLatestRevision(33, 22);
        verifyNoInteractions(manifestMapper);
        verify(indexMapper, never()).switchGeneration(anyLong(), anyLong(), anyLong(), anyString(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void previousBuildCannotDeleteOrCommitCurrentAttempt() {
        ProjectVectorIndex claimedByNewBuild = matchingState();
        claimedByNewBuild.setPendingBuildId("build-b");
        when(indexMapper.selectForUpdate(33)).thenReturn(claimedByNewBuild);

        assertThrows(StaleVectorIndexExecutionException.class,
                () -> service.commit(execution, List.of(), 0));

        verifyNoInteractions(manifestMapper, taskMapper, submissions);
        verify(indexMapper, never()).switchGeneration(anyLong(), anyLong(), anyLong(), anyString(), anyLong(), anyInt(), anyInt());
    }
}

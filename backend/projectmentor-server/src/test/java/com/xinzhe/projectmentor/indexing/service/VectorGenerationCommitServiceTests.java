package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.indexing.entity.VectorFileManifest;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorFileManifestMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class VectorGenerationCommitServiceTests {
    private final ProjectVectorIndexMapper indexMapper = mock(ProjectVectorIndexMapper.class);
    private final VectorIndexTaskMapper taskMapper = mock(VectorIndexTaskMapper.class);
    private final VectorFileManifestMapper manifestMapper = mock(VectorFileManifestMapper.class);
    private final VectorGenerationCommitService service =
            new VectorGenerationCommitService(indexMapper, taskMapper, manifestMapper);
    private final VectorIndexExecutionContext execution =
            new VectorIndexExecutionContext(11, 22, 33, 2, "worker-a", 7, 1);

    @Test
    void switchesGenerationOnlyAfterManifestAndFencedTaskCompletion() {
        VectorFileManifest manifest = new VectorFileManifest();
        when(manifestMapper.insert(manifest)).thenReturn(1);
        when(taskMapper.complete(11, "worker-a", 7)).thenReturn(1);
        when(indexMapper.switchGeneration(33, 22, 2, 9, 1)).thenReturn(1);

        service.commit(execution, List.of(manifest), 9);

        var order = inOrder(manifestMapper, taskMapper, indexMapper);
        order.verify(manifestMapper).deleteGeneration(33, 2);
        order.verify(manifestMapper).insert(manifest);
        order.verify(taskMapper).complete(11, "worker-a", 7);
        order.verify(indexMapper).switchGeneration(33, 22, 2, 9, 1);
    }

    @Test
    void staleWorkerCannotActivatePendingGeneration() {
        when(taskMapper.complete(11, "worker-a", 7)).thenReturn(0);

        assertThrows(StaleVectorIndexExecutionException.class,
                () -> service.commit(execution, List.of(), 0));

        verify(indexMapper, never()).switchGeneration(anyLong(), anyLong(), anyLong(), anyInt(), anyInt());
    }
}

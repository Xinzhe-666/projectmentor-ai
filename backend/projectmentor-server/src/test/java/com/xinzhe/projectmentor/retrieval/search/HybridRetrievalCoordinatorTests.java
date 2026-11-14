package com.xinzhe.projectmentor.retrieval.search;

import com.xinzhe.projectmentor.config.RetrievalProperties;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.indexing.entity.ProjectVectorIndex;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.qa.vo.ProjectQaEvidenceVO;
import com.xinzhe.projectmentor.retrieval.embedding.EmbeddingProvider;
import com.xinzhe.projectmentor.retrieval.milvus.MilvusChunkStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HybridRetrievalCoordinatorTests {
    private final RetrievalProperties properties = new RetrievalProperties();
    private final ProjectVectorIndexMapper indexMapper = mock(ProjectVectorIndexMapper.class);
    private final ProjectFileMapper fileMapper = mock(ProjectFileMapper.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<EmbeddingProvider> embeddings = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<MilvusChunkStore> stores = mock(ObjectProvider.class);
    private final HybridRetrievalCoordinator coordinator = new HybridRetrievalCoordinator(properties, indexMapper,
            fileMapper, embeddings, stores, new EvidenceAwareReranker(), new SimpleMeterRegistry());

    @Test
    void fallsBackWithoutCallingEmbeddingWhenGenerationIsNotReady() {
        properties.setMode(RetrievalProperties.Mode.HYBRID);
        ProjectVectorIndex state = new ProjectVectorIndex();
        state.setStatus("RUNNING");
        when(indexMapper.selectOwned(31, 21)).thenReturn(state);
        var lexical = List.of(ProjectQaEvidenceVO.builder().filePath("README.md").build());

        HybridRetrievalCoordinator.Decision decision = coordinator.retrieve(21, 31, "JWT filter", lexical);

        assertEquals("fallback/lexical", decision.mode());
        assertSame(lexical, decision.evidences());
        assertNotNull(decision.notice());
        assertEquals("fallback/lexical", lexical.get(0).getRetrievalMode());
        verifyNoInteractions(embeddings, stores);
    }

    @Test
    void milvusFailureReturnsLexicalEvidenceInsteadOfThrowing() {
        properties.setMode(RetrievalProperties.Mode.HYBRID);
        ProjectVectorIndex state = new ProjectVectorIndex();
        state.setStatus("READY");
        state.setActiveGeneration(4L);
        when(indexMapper.selectOwned(31, 21)).thenReturn(state);
        EmbeddingProvider embedding = mock(EmbeddingProvider.class);
        MilvusChunkStore store = mock(MilvusChunkStore.class);
        when(embeddings.getIfAvailable()).thenReturn(embedding);
        when(stores.getIfAvailable()).thenReturn(store);
        when(embedding.embed(List.of("JWT filter"))).thenReturn(List.of(new float[]{1, 0, 0, 0}));
        when(embedding.dimension()).thenReturn(4);
        when(store.hybridSearch(21, 31, 4, "JWT filter", new float[]{1, 0, 0, 0}))
                .thenThrow(new IllegalStateException("connection details must stay private"));
        var lexical = List.of(ProjectQaEvidenceVO.builder().filePath("src/Auth.java").build());

        HybridRetrievalCoordinator.Decision decision = coordinator.retrieve(21, 31, "JWT filter", lexical);

        assertEquals("fallback/lexical", decision.mode());
        assertSame(lexical, decision.evidences());
        assertFalse(decision.notice().contains("connection"));
    }
}

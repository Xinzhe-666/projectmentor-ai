package com.xinzhe.projectmentor.retrieval.search;

import com.xinzhe.projectmentor.config.RetrievalProperties;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.indexing.entity.ProjectVectorIndex;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.qa.vo.ProjectQaEvidenceVO;
import com.xinzhe.projectmentor.retrieval.embedding.EmbeddingProvider;
import com.xinzhe.projectmentor.retrieval.chunk.ProjectContentHasher;
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
            fileMapper, embeddings, stores, new EvidenceAwareReranker(), new ProjectContentHasher(),
            new SimpleMeterRegistry());

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
        state.setActiveBuildId("build-ready");
        when(indexMapper.selectOwned(31, 21)).thenReturn(state);
        EmbeddingProvider embedding = mock(EmbeddingProvider.class);
        MilvusChunkStore store = mock(MilvusChunkStore.class);
        when(embeddings.getIfAvailable()).thenReturn(embedding);
        when(stores.getIfAvailable()).thenReturn(store);
        when(embedding.embed(List.of("JWT filter"))).thenReturn(List.of(new float[]{1, 0, 0, 0}));
        when(embedding.dimension()).thenReturn(4);
        when(store.hybridSearch(eq(21L), eq(31L), eq(4L), eq("build-ready"), eq("JWT filter"), any(float[].class)))
                .thenThrow(new IllegalStateException("connection details must stay private"));
        var lexical = List.of(ProjectQaEvidenceVO.builder().filePath("src/Auth.java").build());

        HybridRetrievalCoordinator.Decision decision = coordinator.retrieve(21, 31, "JWT filter", lexical);

        assertEquals("fallback/lexical", decision.mode());
        assertSame(lexical, decision.evidences());
        assertFalse(decision.notice().contains("connection"));
    }

    @Test
    void dropsMissingAndHashMismatchedHitsAndReturnsOnlyCanonicalMysqlEvidence() {
        MilvusChunkStore store = readyStore();
        ProjectFile valid = file(101, "src/Canonical.java", "line one\ncanonical jwt implementation\nline three");
        ProjectFile changed = file(102, "src/Changed.java", "new content");
        when(fileMapper.selectList(any())).thenReturn(List.of(valid, changed));
        when(store.hybridSearch(eq(21L), eq(31L), eq(4L), eq("build-ready"), eq("jwt"), any(float[].class)))
                .thenReturn(List.of(
                        candidate(999, "missing.java", "missing-hash", "untrusted missing payload"),
                        candidate(102, "old-name.java", "old-hash", "untrusted stale payload"),
                        candidate(101, "forged-name.java", new ProjectContentHasher().sha256(valid.getContent()),
                                "forged Milvus payload")));

        HybridRetrievalCoordinator.Decision decision = coordinator.retrieve(21, 31, "jwt", List.of());

        assertEquals("hybrid", decision.mode());
        assertEquals(1, decision.evidences().size());
        ProjectQaEvidenceVO evidence = decision.evidences().get(0);
        assertEquals("src/Canonical.java", evidence.getFilePath());
        assertEquals("canonical jwt implementation", evidence.getSnippet());
        assertFalse(evidence.getSnippet().contains("forged"));
    }

    @Test
    void fallsBackWhenEveryMilvusHitFailsCanonicalValidation() {
        MilvusChunkStore store = readyStore();
        when(fileMapper.selectList(any())).thenReturn(List.of());
        when(store.hybridSearch(eq(21L), eq(31L), eq(4L), eq("build-ready"), eq("jwt"), any(float[].class)))
                .thenReturn(List.of(candidate(999, "missing.java", "missing-hash", "untrusted payload")));
        var lexical = List.of(ProjectQaEvidenceVO.builder().filePath("README.md").build());

        HybridRetrievalCoordinator.Decision decision = coordinator.retrieve(21, 31, "jwt", lexical);

        assertEquals("fallback/lexical", decision.mode());
        assertSame(lexical, decision.evidences());
        assertEquals("fallback/lexical", lexical.get(0).getRetrievalMode());
    }

    private MilvusChunkStore readyStore() {
        properties.setMode(RetrievalProperties.Mode.HYBRID);
        ProjectVectorIndex state = new ProjectVectorIndex();
        state.setStatus("READY");
        state.setActiveGeneration(4L);
        state.setActiveBuildId("build-ready");
        when(indexMapper.selectOwned(31, 21)).thenReturn(state);
        EmbeddingProvider embedding = mock(EmbeddingProvider.class);
        MilvusChunkStore store = mock(MilvusChunkStore.class);
        when(embeddings.getIfAvailable()).thenReturn(embedding);
        when(stores.getIfAvailable()).thenReturn(store);
        when(embedding.embed(List.of("jwt"))).thenReturn(List.of(new float[]{1, 0, 0, 0}));
        when(embedding.dimension()).thenReturn(4);
        return store;
    }

    private ProjectFile file(long id, String path, String content) {
        ProjectFile file = new ProjectFile();
        file.setId(id); file.setProjectId(31L); file.setFilePath(path); file.setContent(content);
        return file;
    }

    private RetrievalCandidate candidate(long fileId, String path, String hash, String content) {
        return new RetrievalCandidate("chunk-" + fileId, fileId, path, "java", "CODE", "JwtService",
                2, 2, hash, content, null, null, null, null, 1, 0.03, 0, 0, List.of(), List.of());
    }
}

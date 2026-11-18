package com.xinzhe.projectmentor.retrieval.milvus;

import com.xinzhe.projectmentor.config.MilvusProperties;
import com.xinzhe.projectmentor.config.RetrievalProperties;
import com.xinzhe.projectmentor.retrieval.chunk.CodeChunk;
import com.xinzhe.projectmentor.retrieval.chunk.SensitiveContentRedactor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.HybridSearchReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.response.SearchResp;
import io.milvus.v2.service.vector.response.UpsertResp;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MilvusChunkStoreTests {
    @Test
    void storesOnlyRedactedPreviewInsteadOfInlineSensitiveValue() {
        MilvusClientV2 client = mock(MilvusClientV2.class);
        when(client.upsert(any(UpsertReq.class))).thenReturn(UpsertResp.builder().upsertCnt(1).build());
        MilvusChunkStore store = store(client, false);
        CodeChunk chunk = new CodeChunk("logical", 2L, "src/Secrets.java", "java", "CODE", "Secrets",
                1, 1, "hash", "api_key=supersecretvalue", "api key", "api key");

        assertThat(store.insert(7, 8, 9, "build-secret", List.of(chunk),
                List.of(new float[]{1, 0, 0, 0}))).isEqualTo(1);

        ArgumentCaptor<UpsertReq> request = ArgumentCaptor.forClass(UpsertReq.class);
        verify(client).upsert(request.capture());
        String preview = request.getValue().getData().get(0).get("content").getAsString();
        assertThat(preview).contains("[REDACTED]").doesNotContain("supersecretvalue");
        assertThat(request.getValue().getData().get(0).get("build_id").getAsString()).isEqualTo("build-secret");
    }

    @Test
    void preservesAuthoritativeServerRrfOrderWithoutClientSideRefusion() {
        MilvusClientV2 client = mock(MilvusClientV2.class);
        SearchResp response = SearchResp.builder().searchResults(List.of(List.of(
                hit("server-second-source", 2, 0.041f),
                hit("server-first-source", 1, 0.037f)))).build();
        when(client.hybridSearch(any(HybridSearchReq.class))).thenReturn(response);
        MilvusChunkStore store = store(client, false);

        var result = store.hybridSearch(7, 8, 9, "build-b", "jwt", new float[]{1, 0, 0, 0});

        assertThat(result).extracting(candidate -> candidate.chunkId())
                .containsExactly("server-second-source", "server-first-source");
        assertThat(result).extracting(candidate -> candidate.rrfRank()).containsExactly(1, 2);
        assertThat(result.get(0).rrfScore()).isCloseTo(0.041d, within(0.000001d));
        assertThat(result.get(1).rrfScore()).isCloseTo(0.037d, within(0.000001d));
        verify(client, never()).search(any(SearchReq.class));
        ArgumentCaptor<HybridSearchReq> request = ArgumentCaptor.forClass(HybridSearchReq.class);
        verify(client).hybridSearch(request.capture());
        assertThat(request.getValue().getRanker()).isNotNull();
        assertThat(request.getValue().getSearchRequests()).hasSize(2);
        assertThat(request.getValue().getSearchRequests())
                .extracting(value -> value.getVectorFieldName()).containsExactly("dense_vector", "sparse_vector");
    }

    private MilvusChunkStore store(MilvusClientV2 client, boolean diagnostics) {
        MilvusProperties milvus = new MilvusProperties();
        milvus.setDatabase("pmai"); milvus.setCollection("chunks");
        RetrievalProperties retrieval = new RetrievalProperties();
        retrieval.setCandidateLimit(10); retrieval.setRrfK(60);
        retrieval.setPerPathDiagnosticsEnabled(diagnostics);
        return new MilvusChunkStore(client, milvus, retrieval,
                new SensitiveContentRedactor(), new SimpleMeterRegistry());
    }

    private SearchResp.SearchResult hit(String id, long fileId, float score) {
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("chunk_id", id); entity.put("file_id", fileId); entity.put("file_path", "src/" + id + ".java");
        entity.put("language", "java"); entity.put("chunk_type", "CODE"); entity.put("symbol_name", "Service");
        entity.put("start_line", 1); entity.put("end_line", 2); entity.put("content_hash", "hash-" + id);
        entity.put("content", "redacted preview");
        return SearchResp.SearchResult.builder().primaryKey(id).score(score).entity(entity).build();
    }
}

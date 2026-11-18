package com.xinzhe.projectmentor.retrieval.milvus;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xinzhe.projectmentor.config.MilvusProperties;
import com.xinzhe.projectmentor.config.RetrievalProperties;
import com.xinzhe.projectmentor.retrieval.chunk.CodeChunk;
import com.xinzhe.projectmentor.retrieval.chunk.SensitiveContentRedactor;
import com.xinzhe.projectmentor.retrieval.search.RetrievalCandidate;
import io.micrometer.core.instrument.MeterRegistry;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.vector.request.AnnSearchReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.HybridSearchReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.EmbeddedText;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.request.ranker.RRFRanker;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.response.UpsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "projectmentor.milvus", name = "enabled", havingValue = "true")
public class MilvusChunkStore {
    private static final List<String> OUTPUT_FIELDS = List.of("chunk_id", "file_id", "file_path", "language",
            "chunk_type", "symbol_name", "start_line", "end_line", "content_hash", "content");
    private static final String FILTER = "user_id == {userId} and project_id == {projectId} "
            + "and generation == {generation} and build_id == {buildId}";

    private final MilvusClientV2 client;
    private final MilvusProperties milvus;
    private final RetrievalProperties retrieval;
    private final SensitiveContentRedactor redactor;
    private final MeterRegistry metrics;

    public long insert(long userId, long projectId, long generation, String buildId,
                       List<CodeChunk> chunks, List<float[]> vectors) {
        if (chunks.size() != vectors.size()) throw new IllegalArgumentException("Chunk/vector count mismatch");
        List<JsonObject> rows = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            rows.add(row(userId, projectId, generation, buildId, chunks.get(i), vectors.get(i)));
        }
        long started = System.nanoTime();
        try {
            UpsertResp response = client.upsert(UpsertReq.builder().databaseName(milvus.getDatabase())
                    .collectionName(milvus.getCollection()).data(rows).build());
            metrics.counter("pmai.milvus.insert", "result", "success").increment(response.getUpsertCnt());
            return response.getUpsertCnt();
        } catch (RuntimeException e) {
            metrics.counter("pmai.milvus.error", "operation", "insert").increment();
            throw e;
        } finally {
            metrics.timer("pmai.milvus.insert.latency").record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
        }
    }

    public List<RetrievalCandidate> hybridSearch(long userId, long projectId, long generation, String buildId,
                                                  String query, float[] denseVector) {
        Map<String, Object> values = Map.of("userId", userId, "projectId", projectId,
                "generation", generation, "buildId", buildId);
        int limit = retrieval.getCandidateLimit();
        AnnSearchReq dense = AnnSearchReq.builder().vectorFieldName("dense_vector")
                .vectors(List.of(new FloatVec(denseVector))).metricType(IndexParam.MetricType.COSINE)
                .filter(FILTER).filterTemplateValues(values).limit(limit).build();
        AnnSearchReq sparse = AnnSearchReq.builder().vectorFieldName("sparse_vector")
                .vectors(List.of(new EmbeddedText(query))).metricType(IndexParam.MetricType.BM25)
                .filter(FILTER).filterTemplateValues(values).limit(limit).build();

        long started = System.nanoTime();
        try {
            // The server-side request is the authoritative Dense+BM25 hybrid operation and uses RRFRanker.
            SearchResp hybrid = client.hybridSearch(HybridSearchReq.builder().databaseName(milvus.getDatabase())
                    .collectionName(milvus.getCollection()).searchRequests(List.of(dense, sparse))
                    .ranker(RRFRanker.builder().k(retrieval.getRrfK()).build())
                    .consistencyLevel(ConsistencyLevel.STRONG)
                    .limit(limit).outFields(OUTPUT_FIELDS).build());
            List<RetrievalCandidate> serverRanked = serverRanked(hybrid);
            if (retrieval.isPerPathDiagnosticsEnabled() && !serverRanked.isEmpty()) {
                List<RetrievalCandidate> denseResults = searchPath("dense_vector", IndexParam.MetricType.COSINE,
                        new FloatVec(denseVector), values, limit, true);
                List<RetrievalCandidate> sparseResults = searchPath("sparse_vector", IndexParam.MetricType.BM25,
                        new EmbeddedText(query), values, limit, false);
                serverRanked = attachDiagnostics(serverRanked, denseResults, sparseResults);
            }
            metrics.counter("pmai.milvus.search", "result", "success").increment();
            return serverRanked;
        } catch (RuntimeException e) {
            metrics.counter("pmai.milvus.error", "operation", "search").increment();
            throw e;
        } finally {
            metrics.timer("pmai.hybrid.retrieval.latency").record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
        }
    }

    public void deleteBuild(long userId, long projectId, long generation, String buildId) {
        client.delete(DeleteReq.builder().databaseName(milvus.getDatabase()).collectionName(milvus.getCollection())
                .filter(FILTER).filterTemplateValues(Map.of("userId", userId, "projectId", projectId,
                        "generation", generation, "buildId", buildId)).build());
    }

    public void deleteProject(long userId, long projectId) {
        client.delete(DeleteReq.builder().databaseName(milvus.getDatabase()).collectionName(milvus.getCollection())
                .filter("user_id == {userId} and project_id == {projectId}")
                .filterTemplateValues(Map.of("userId", userId, "projectId", projectId)).build());
    }

    private List<RetrievalCandidate> searchPath(String field, IndexParam.MetricType metric, Object vector,
                                                 Map<String, Object> values, int limit, boolean dense) {
        SearchResp response = client.search(SearchReq.builder().databaseName(milvus.getDatabase())
                .collectionName(milvus.getCollection()).annsField(field).metricType(metric)
                .data(List.of((io.milvus.v2.service.vector.request.data.BaseVector) vector))
                .consistencyLevel(ConsistencyLevel.STRONG)
                .filter(FILTER).filterTemplateValues(values).limit(limit).outputFields(OUTPUT_FIELDS).build());
        if (response.getSearchResults().isEmpty()) return List.of();
        List<RetrievalCandidate> result = new ArrayList<>();
        for (SearchResp.SearchResult hit : response.getSearchResults().get(0)) {
            result.add(toCandidate(hit, dense));
        }
        return result;
    }

    private List<RetrievalCandidate> serverRanked(SearchResp response) {
        if (response.getSearchResults().isEmpty()) return List.of();
        List<RetrievalCandidate> result = new ArrayList<>();
        List<SearchResp.SearchResult> hits = response.getSearchResults().get(0);
        for (int i = 0; i < hits.size(); i++) {
            RetrievalCandidate base = toCandidate(hits.get(i), null);
            result.add(base.withFusion(null, null, null, null, i + 1, hits.get(i).getScore()));
        }
        return result;
    }

    private List<RetrievalCandidate> attachDiagnostics(List<RetrievalCandidate> serverRanked,
                                                       List<RetrievalCandidate> dense,
                                                       List<RetrievalCandidate> bm25) {
        Map<String, RetrievalCandidate> denseById = rankedById(dense, true);
        Map<String, RetrievalCandidate> bm25ById = rankedById(bm25, false);
        List<RetrievalCandidate> result = new ArrayList<>(serverRanked.size());
        for (RetrievalCandidate server : serverRanked) {
            RetrievalCandidate d = denseById.get(server.chunkId());
            RetrievalCandidate b = bm25ById.get(server.chunkId());
            result.add(server.withFusion(d == null ? null : d.denseRank(), d == null ? null : d.denseScore(),
                    b == null ? null : b.bm25Rank(), b == null ? null : b.bm25Score(),
                    server.rrfRank(), server.rrfScore()));
        }
        return result;
    }

    private Map<String, RetrievalCandidate> rankedById(List<RetrievalCandidate> candidates, boolean dense) {
        Map<String, RetrievalCandidate> result = new HashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            RetrievalCandidate candidate = candidates.get(i);
            result.put(candidate.chunkId(), candidate.withFusion(dense ? i + 1 : null,
                    dense ? candidate.denseScore() : null, dense ? null : i + 1,
                    dense ? null : candidate.bm25Score(), 0, 0));
        }
        return result;
    }

    private RetrievalCandidate toCandidate(SearchResp.SearchResult hit, Boolean dense) {
        Map<String, Object> e = hit.getEntity();
        return new RetrievalCandidate(string(e, "chunk_id", hit.getPrimaryKey()), number(e, "file_id").longValue(),
                string(e, "file_path", ""), string(e, "language", "text"), string(e, "chunk_type", "CODE"),
                string(e, "symbol_name", "file-scope"), number(e, "start_line").intValue(),
                number(e, "end_line").intValue(), string(e, "content_hash", ""), string(e, "content", ""),
                null, Boolean.TRUE.equals(dense) ? hit.getScore() : null,
                null, Boolean.FALSE.equals(dense) ? hit.getScore() : null,
                0, 0, 0, 0, List.of(), List.of());
    }

    private JsonObject row(long userId, long projectId, long generation, String buildId,
                           CodeChunk chunk, float[] vector) {
        JsonObject row = new JsonObject();
        row.addProperty("chunk_id", buildChunkId(chunk.chunkId(), generation, buildId)); row.addProperty("user_id", userId);
        row.addProperty("project_id", projectId); row.addProperty("file_id", chunk.fileId());
        row.addProperty("generation", generation); row.addProperty("build_id", buildId);
        row.addProperty("file_path", chunk.filePath());
        row.addProperty("language", chunk.language()); row.addProperty("chunk_type", chunk.chunkType());
        row.addProperty("symbol_name", chunk.symbolName()); row.addProperty("start_line", chunk.startLine());
        row.addProperty("end_line", chunk.endLine()); row.addProperty("content_hash", chunk.contentHash());
        String preview = redactor.redact(chunk.content()).content();
        row.addProperty("content", preview.substring(0, Math.min(preview.length(), 65535)));
        row.addProperty("search_text", chunk.searchText());
        JsonArray dense = new JsonArray(vector.length); for (float value : vector) dense.add(value);
        row.add("dense_vector", dense);
        return row;
    }

    private String string(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key); return value == null ? fallback : String.valueOf(value);
    }
    private Number number(Map<String, Object> values, String key) {
        Object value = values.get(key); return value instanceof Number n ? n : Long.parseLong(String.valueOf(value));
    }

    private String buildChunkId(String logicalChunkId, long generation, String buildId) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((logicalChunkId + ":" + generation + ":" + buildId)
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

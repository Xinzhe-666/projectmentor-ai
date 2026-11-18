package com.xinzhe.projectmentor.retrieval.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.config.RetrievalProperties;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import com.xinzhe.projectmentor.file.mapper.ProjectFileMapper;
import com.xinzhe.projectmentor.indexing.entity.ProjectVectorIndex;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.qa.vo.ProjectQaEvidenceVO;
import com.xinzhe.projectmentor.retrieval.chunk.ProjectContentHasher;
import com.xinzhe.projectmentor.retrieval.embedding.EmbeddingProvider;
import com.xinzhe.projectmentor.retrieval.milvus.MilvusChunkStore;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HybridRetrievalCoordinator {
    private final RetrievalProperties properties;
    private final ProjectVectorIndexMapper indexMapper;
    private final ProjectFileMapper fileMapper;
    private final ObjectProvider<EmbeddingProvider> embeddingProvider;
    private final ObjectProvider<MilvusChunkStore> chunkStoreProvider;
    private final EvidenceAwareReranker reranker;
    private final ProjectContentHasher contentHasher;
    private final MeterRegistry metrics;

    public Decision retrieve(long userId, long projectId, String query, List<ProjectQaEvidenceVO> lexical) {
        metrics.counter("pmai.retrieval.mode", "mode", properties.getMode().name().toLowerCase()).increment();
        if (properties.getMode() == RetrievalProperties.Mode.LEXICAL) {
            stamp(lexical, "lexical");
            return new Decision(lexical, "lexical", null);
        }
        long started = System.nanoTime();
        try {
            ProjectVectorIndex state = indexMapper.selectOwned(projectId, userId);
            if (state == null || !"READY".equals(state.getStatus()) || state.getActiveGeneration() == null
                    || state.getActiveBuildId() == null) {
                return fallback(lexical, "项目索引尚未可用");
            }
            EmbeddingProvider embedding = embeddingProvider.getIfAvailable();
            MilvusChunkStore store = chunkStoreProvider.getIfAvailable();
            if (embedding == null || store == null) return fallback(lexical, "混合检索服务未启用");
            List<float[]> queryVectors = embedding.embed(List.of(query));
            if (queryVectors.size() != 1 || queryVectors.get(0).length != embedding.dimension()) {
                return fallback(lexical, "查询向量校验失败");
            }
            List<RetrievalCandidate> fused = store.hybridSearch(userId, projectId,
                    state.getActiveGeneration(), state.getActiveBuildId(), query, queryVectors.get(0));
            List<RetrievalCandidate> canonical = canonicalize(projectId, fused);
            List<RetrievalCandidate> reranked = reranker.rerank(query, canonical, properties.getResultLimit());
            List<ProjectQaEvidenceVO> hybrid = hydrate(reranked);
            if (hybrid.isEmpty()) return fallback(lexical, "混合检索未返回可核验证据");
            if (properties.getMode() == RetrievalProperties.Mode.SHADOW) {
                metrics.summary("pmai.retrieval.shadow.result.delta").record(hybrid.size() - lexical.size());
                stamp(lexical, "shadow/lexical");
                return new Decision(lexical, "shadow/lexical", "已后台比较混合检索，当前仍返回基础检索结果");
            }
            return new Decision(hybrid, "hybrid", null);
        } catch (Exception e) {
            log.warn("hybrid_retrieval_fallback reason={}", safeReason(e));
            return fallback(lexical, "混合证据检索暂不可用，已自动使用基础检索");
        } finally {
            metrics.timer("pmai.retrieval.coordinator.latency")
                    .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
        }
    }

    private Decision fallback(List<ProjectQaEvidenceVO> lexical, String notice) {
        stamp(lexical, "fallback/lexical");
        metrics.counter("pmai.retrieval.lexical.fallback", "reason", lowCardinality(notice)).increment();
        return new Decision(lexical, "fallback/lexical", notice);
    }

    private List<RetrievalCandidate> canonicalize(long projectId, List<RetrievalCandidate> candidates) {
        List<Long> ids = candidates.stream().map(RetrievalCandidate::fileId).distinct().toList();
        Map<Long, ProjectFile> files = new HashMap<>();
        if (!ids.isEmpty()) {
            for (ProjectFile file : fileMapper.selectList(new LambdaQueryWrapper<ProjectFile>()
                    .eq(ProjectFile::getProjectId, projectId).in(ProjectFile::getId, ids))) {
                files.put(file.getId(), file);
            }
        }
        List<RetrievalCandidate> result = new ArrayList<>();
        for (RetrievalCandidate candidate : candidates) {
            ProjectFile canonical = files.get(candidate.fileId());
            if (canonical == null || !contentHasher.sha256(canonical.getContent()).equals(candidate.contentHash())) {
                metrics.counter("pmai.retrieval.stale_evidence", "reason",
                        canonical == null ? "file-missing" : "hash-mismatch").increment();
                continue;
            }
            String snippet = lines(canonical.getContent(), candidate.startLine(), candidate.endLine());
            if (snippet == null || snippet.isBlank()) continue;
            result.add(new RetrievalCandidate(candidate.chunkId(), candidate.fileId(), canonical.getFilePath(),
                    candidate.language(), candidate.chunkType(), candidate.symbolName(), candidate.startLine(),
                    candidate.endLine(), candidate.contentHash(), snippet, candidate.denseRank(), candidate.denseScore(),
                    candidate.bm25Rank(), candidate.bm25Score(), candidate.rrfRank(), candidate.rrfScore(),
                    candidate.evidenceBoost(), candidate.finalRank(), candidate.matchedTerms(), candidate.matchReasons()));
        }
        return result;
    }

    private List<ProjectQaEvidenceVO> hydrate(List<RetrievalCandidate> candidates) {
        List<ProjectQaEvidenceVO> result = new ArrayList<>();
        for (RetrievalCandidate candidate : candidates) {
            String explanation = String.join("; ", candidate.matchReasons());
            result.add(ProjectQaEvidenceVO.builder().filePath(candidate.filePath())
                    .reason(explanation.isBlank() ? "Dense + BM25 经 RRF 融合命中" : explanation)
                    .snippet(candidate.content()).startLine(candidate.startLine()).endLine(candidate.endLine())
                    .chunkType(candidate.chunkType()).language(candidate.language())
                    .matchedTerms(candidate.matchedTerms()).retrievalMode("hybrid")
                    .retrievalExplanation(explanation).denseRank(candidate.denseRank()).denseScore(candidate.denseScore())
                    .bm25Rank(candidate.bm25Rank()).bm25Score(candidate.bm25Score()).rrfRank(candidate.rrfRank())
                    .evidenceBoost(candidate.evidenceBoost()).finalRank(candidate.finalRank()).build());
        }
        return result;
    }

    private String lines(String content, int startLine, int endLine) {
        String[] lines = contentHasher.normalize(content).split("\n", -1);
        if (startLine < 1 || startLine > lines.length || endLine < startLine) return null;
        int from = startLine - 1;
        int to = Math.min(lines.length, endLine);
        return String.join("\n", java.util.Arrays.copyOfRange(lines, from, to));
    }

    private void stamp(List<ProjectQaEvidenceVO> evidences, String mode) {
        for (ProjectQaEvidenceVO evidence : evidences) evidence.setRetrievalMode(mode);
    }
    private String safeReason(Exception e) { return e.getClass().getSimpleName(); }
    private String lowCardinality(String notice) {
        if (notice.contains("索引")) return "index-not-ready";
        if (notice.contains("向量")) return "embedding";
        if (notice.contains("证据")) return "no-evidence";
        return "unavailable";
    }
    public record Decision(List<ProjectQaEvidenceVO> evidences, String mode, String notice) {}
}

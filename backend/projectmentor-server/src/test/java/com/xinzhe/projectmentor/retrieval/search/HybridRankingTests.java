package com.xinzhe.projectmentor.retrieval.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HybridRankingTests {
    private final ReciprocalRankFusion fusion = new ReciprocalRankFusion();
    private final EvidenceAwareReranker reranker = new EvidenceAwareReranker();

    @Test
    void rrfCombinesRanksWithoutAddingIncompatibleRawScores() {
        RetrievalCandidate aDense = candidate("a", "README.md", "DOC", "overview", 0.91f, null);
        RetrievalCandidate bDense = candidate("b", "src/JwtAuthenticationFilter.java", "CODE", "JwtAuthenticationFilter", 0.88f, null);
        RetrievalCandidate bBm25 = candidate("b", "src/JwtAuthenticationFilter.java", "CODE", "JwtAuthenticationFilter", null, 18f);
        RetrievalCandidate cBm25 = candidate("c", "application.yml", "CONFIG", "security", null, 15f);

        List<RetrievalCandidate> result = fusion.fuse(List.of(aDense, bDense), List.of(bBm25, cBm25), 60);

        assertThat(result).extracting(RetrievalCandidate::chunkId).containsExactly("b", "a", "c");
        assertThat(result.get(0).denseRank()).isEqualTo(2);
        assertThat(result.get(0).bm25Rank()).isEqualTo(1);
    }

    @Test
    void evidenceRerankPromotesExactImplementationAndPenalizesRepeatedDocs() {
        List<RetrievalCandidate> fused = fusion.fuse(
                List.of(candidate("doc", "README.md", "DOC", "overview", 0.9f, null),
                        candidate("code", "src/JwtAuthenticationFilter.java", "CODE", "JwtAuthenticationFilter", 0.8f, null)),
                List.of(candidate("code", "src/JwtAuthenticationFilter.java", "CODE", "JwtAuthenticationFilter", null, 22f)), 60);

        List<RetrievalCandidate> result = reranker.rerank("JwtAuthenticationFilter 在哪里", fused, 5);

        assertThat(result.get(0).chunkId()).isEqualTo("code");
        assertThat(result.get(0).matchedTerms()).contains("jwtauthenticationfilter");
        assertThat(result.get(0).matchReasons()).contains("implementation-evidence");
    }

    private RetrievalCandidate candidate(String id, String path, String type, String symbol,
                                         Float dense, Float bm25) {
        return new RetrievalCandidate(id, 1L, path, "java", type, symbol, 1, 20, id + "-hash",
                "class " + symbol + " {}", null, dense, null, bm25, 0, 0, 0, 0, List.of(), List.of());
    }
}

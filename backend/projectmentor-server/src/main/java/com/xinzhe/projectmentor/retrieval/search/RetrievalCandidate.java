package com.xinzhe.projectmentor.retrieval.search;

import java.util.List;

public record RetrievalCandidate(
        String chunkId,
        Long fileId,
        String filePath,
        String language,
        String chunkType,
        String symbolName,
        int startLine,
        int endLine,
        String contentHash,
        String content,
        Integer denseRank,
        Float denseScore,
        Integer bm25Rank,
        Float bm25Score,
        int rrfRank,
        double rrfScore,
        double evidenceBoost,
        int finalRank,
        List<String> matchedTerms,
        List<String> matchReasons
) {
    public RetrievalCandidate withFusion(Integer newDenseRank, Float newDenseScore,
                                         Integer newBm25Rank, Float newBm25Score,
                                         int newRrfRank, double newRrfScore) {
        return new RetrievalCandidate(chunkId, fileId, filePath, language, chunkType, symbolName,
                startLine, endLine, contentHash, content, newDenseRank, newDenseScore,
                newBm25Rank, newBm25Score, newRrfRank, newRrfScore, evidenceBoost, finalRank,
                matchedTerms, matchReasons);
    }

    public RetrievalCandidate withRerank(double boost, int rank, List<String> terms, List<String> reasons) {
        return new RetrievalCandidate(chunkId, fileId, filePath, language, chunkType, symbolName,
                startLine, endLine, contentHash, content, denseRank, denseScore, bm25Rank, bm25Score,
                rrfRank, rrfScore, boost, rank, terms, reasons);
    }
}

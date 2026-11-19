package com.xinzhe.projectmentor.retrieval.search;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReciprocalRankFusion {
    public List<RetrievalCandidate> fuse(List<RetrievalCandidate> dense,
                                         List<RetrievalCandidate> bm25,
                                         int k) {
        Map<String, MutableFusion> combined = new LinkedHashMap<>();
        for (int i = 0; i < dense.size(); i++) {
            RetrievalCandidate item = dense.get(i);
            MutableFusion state = combined.computeIfAbsent(item.chunkId(), ignored -> new MutableFusion(item));
            state.denseRank = i + 1;
            state.denseScore = item.denseScore();
            state.rrfScore += 1.0d / (k + i + 1.0d);
        }
        for (int i = 0; i < bm25.size(); i++) {
            RetrievalCandidate item = bm25.get(i);
            MutableFusion state = combined.computeIfAbsent(item.chunkId(), ignored -> new MutableFusion(item));
            state.bm25Rank = i + 1;
            state.bm25Score = item.bm25Score();
            state.rrfScore += 1.0d / (k + i + 1.0d);
        }
        List<MutableFusion> sorted = new ArrayList<>(combined.values());
        sorted.sort(Comparator.comparingDouble((MutableFusion value) -> value.rrfScore).reversed()
                .thenComparing(value -> value.base.chunkId()));
        List<RetrievalCandidate> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            MutableFusion value = sorted.get(i);
            result.add(value.base.withFusion(value.denseRank, value.denseScore, value.bm25Rank,
                    value.bm25Score, i + 1, value.rrfScore));
        }
        return result;
    }

    private static final class MutableFusion {
        private final RetrievalCandidate base;
        private Integer denseRank;
        private Float denseScore;
        private Integer bm25Rank;
        private Float bm25Score;
        private double rrfScore;
        private MutableFusion(RetrievalCandidate base) { this.base = base; }
    }
}

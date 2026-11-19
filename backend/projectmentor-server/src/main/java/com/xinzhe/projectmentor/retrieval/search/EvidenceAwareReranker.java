package com.xinzhe.projectmentor.retrieval.search;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class EvidenceAwareReranker {
    private static final Pattern SPLIT = Pattern.compile("[^A-Za-z0-9_./-]+", Pattern.UNICODE_CASE);
    private static final Set<String> STOP = Set.of("the", "and", "what", "where", "how", "why", "is", "are",
            "项目", "代码", "实现", "哪里", "如何", "什么");

    public List<RetrievalCandidate> rerank(String query, List<RetrievalCandidate> fused, int limit) {
        Set<String> terms = terms(query);
        Map<String, Integer> fileCount = new HashMap<>();
        List<Scored> scored = new ArrayList<>();
        for (RetrievalCandidate item : fused) {
            String path = lower(item.filePath());
            String symbol = lower(item.symbolName());
            String content = lower(item.content());
            List<String> matched = new ArrayList<>();
            List<String> reasons = new ArrayList<>();
            double boost = 0;
            for (String term : terms) {
                boolean found = false;
                if (path.contains(term)) { boost += 0.025; found = true; reasons.add("path:" + term); }
                if (symbol.contains(term)) { boost += 0.04; found = true; reasons.add("symbol:" + term); }
                if (content.contains(term)) { boost += 0.012; found = true; }
                if (found) matched.add(term);
            }
            if (Set.of("CODE", "TYPE", "FUNCTION", "CONFIG", "SQL", "DEPLOY").contains(item.chunkType())) {
                boost += 0.018;
                reasons.add("implementation-evidence");
            } else if ("DOC".equals(item.chunkType()) && !terms.stream().anyMatch(path::contains)) {
                boost -= 0.015;
                reasons.add("doc-only-penalty");
            }
            int duplicateIndex = fileCount.merge(item.filePath(), 1, Integer::sum) - 1;
            if (duplicateIndex > 0) {
                boost -= Math.min(0.04, duplicateIndex * 0.012);
                reasons.add("same-file-duplicate-penalty");
            }
            double completeness = Math.min(0.012, Math.max(0, item.endLine() - item.startLine()) / 5000.0);
            boost += completeness;
            scored.add(new Scored(item, boost, List.copyOf(new LinkedHashSet<>(matched)),
                    List.copyOf(new LinkedHashSet<>(reasons))));
        }
        scored.sort(Comparator.comparingDouble((Scored value) -> value.item.rrfScore() + value.boost).reversed()
                .thenComparingInt(value -> value.item.rrfRank()));
        List<RetrievalCandidate> result = new ArrayList<>();
        Set<String> seenHashes = new LinkedHashSet<>();
        for (Scored value : scored) {
            if (!seenHashes.add(value.item.contentHash() + ":" + value.item.startLine() + ":" + value.item.endLine())) {
                continue;
            }
            result.add(value.item.withRerank(value.boost, result.size() + 1, value.terms, value.reasons));
            if (result.size() >= limit) break;
        }
        return result;
    }

    private Set<String> terms(String query) {
        Set<String> result = new LinkedHashSet<>();
        for (String raw : SPLIT.split(lower(query).replace('_', ' '))) {
            if (raw.length() >= 2 && !STOP.contains(raw)) result.add(raw);
        }
        return result;
    }

    private String lower(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
    private record Scored(RetrievalCandidate item, double boost, List<String> terms, List<String> reasons) {}
}

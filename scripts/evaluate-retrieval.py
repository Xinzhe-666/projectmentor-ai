#!/usr/bin/env python3
"""Deterministic PMAI retrieval process evaluator; mock dense vectors are not quality evidence."""
from __future__ import annotations

import json
import math
import re
import statistics
import sys
import time
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / "evaluation" / "fixtures"
LABELS = ROOT / "evaluation" / "retrieval-labels.json"
TOKEN = re.compile(r"[A-Za-z][A-Za-z0-9]*|[\u4e00-\u9fff]+")


def tokens(value: str) -> list[str]:
    expanded = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", value.replace("_", " ").replace("/", " "))
    return [part.lower() for part in TOKEN.findall(expanded) if len(part) > 1]


def documents(project: str) -> dict[str, list[str]]:
    base = FIXTURES / project
    return {path.relative_to(base).as_posix(): tokens(path.relative_to(base).as_posix() + "\n" + path.read_text(encoding="utf-8"))
            for path in sorted(base.rglob("*")) if path.is_file()}


def lexical(query: list[str], docs: dict[str, list[str]]) -> dict[str, float]:
    q = Counter(query)
    return {path: sum(min(count, Counter(words)[term]) for term, count in q.items()) for path, words in docs.items()}


def bm25(query: list[str], docs: dict[str, list[str]]) -> dict[str, float]:
    size = len(docs); avg = sum(map(len, docs.values())) / max(size, 1)
    dfs = Counter(term for words in docs.values() for term in set(words))
    scores = {}
    for path, words in docs.items():
        tf = Counter(words); score = 0.0
        for term in query:
            freq = tf[term]
            if not freq: continue
            idf = math.log(1 + (size - dfs[term] + 0.5) / (dfs[term] + 0.5))
            score += idf * freq * 2.2 / (freq + 1.2 * (0.25 + 0.75 * len(words) / avg))
        scores[path] = score
    return scores


def vector(words: list[str], dimensions: int = 128) -> list[float]:
    result = [0.0] * dimensions
    for word in words:
        # Stable feature hashing is deliberately a CI process double, not a semantic embedding model.
        slot = int.from_bytes(word.encode("utf-8"), "little", signed=False) % dimensions
        result[slot] += 1.0
    norm = math.sqrt(sum(value * value for value in result)) or 1.0
    return [value / norm for value in result]


def dense(query: list[str], docs: dict[str, list[str]]) -> dict[str, float]:
    q = vector(query)
    return {path: sum(a * b for a, b in zip(q, vector(words))) for path, words in docs.items()}


def ranked(scores: dict[str, float]) -> list[str]:
    return [path for path, score in sorted(scores.items(), key=lambda item: (-item[1], item[0])) if score > 0]


def rrf(first: list[str], second: list[str], k: int = 60) -> dict[str, float]:
    result = Counter()
    for ranking in (first, second):
        for position, path in enumerate(ranking, 1): result[path] += 1 / (k + position)
    return dict(result)


def evidence_rerank(query: list[str], scores: dict[str, float], docs: dict[str, list[str]]) -> dict[str, float]:
    result = {}
    for path, score in scores.items():
        path_tokens = set(tokens(path)); exact = len(path_tokens.intersection(query))
        implementation = 0.004 if Path(path).suffix.lower() in {".java", ".ts", ".vue", ".sql", ".yml", ".yaml"} else 0
        doc_penalty = 0.003 if path.lower().endswith("readme.md") else 0
        result[path] = score + exact * 0.002 + implementation - doc_penalty
    return result


def ndcg(ranking: list[str], relevant: set[str], k: int = 10) -> float:
    dcg = sum(1 / math.log2(index + 2) for index, path in enumerate(ranking[:k]) if path in relevant)
    ideal = sum(1 / math.log2(index + 2) for index in range(min(k, len(relevant))))
    return dcg / ideal if ideal else 1.0


def percentile(values: list[float], ratio: float) -> float:
    ordered = sorted(values); index = max(0, math.ceil(len(ordered) * ratio) - 1)
    return ordered[index]


def main() -> int:
    labels = json.loads(LABELS.read_text(encoding="utf-8")); cache = {}
    results = {name: [] for name in ("lexical", "dense_mock", "bm25", "hybrid_rrf", "hybrid_evidence")}
    latencies = {name: [] for name in results}
    for case in labels:
        docs = cache.setdefault(case["project"], documents(case["project"])); query = tokens(case["query"])
        started = time.perf_counter_ns(); lexical_scores = lexical(query, docs); latencies["lexical"].append((time.perf_counter_ns()-started)/1e6)
        started = time.perf_counter_ns(); dense_scores = dense(query, docs); latencies["dense_mock"].append((time.perf_counter_ns()-started)/1e6)
        started = time.perf_counter_ns(); bm25_scores = bm25(query, docs); latencies["bm25"].append((time.perf_counter_ns()-started)/1e6)
        started = time.perf_counter_ns(); hybrid_scores = rrf(ranked(dense_scores), ranked(bm25_scores)); latencies["hybrid_rrf"].append((time.perf_counter_ns()-started)/1e6)
        started = time.perf_counter_ns(); evidence_scores = evidence_rerank(query, hybrid_scores, docs); latencies["hybrid_evidence"].append((time.perf_counter_ns()-started)/1e6)
        for name, scores in (("lexical", lexical_scores), ("dense_mock", dense_scores), ("bm25", bm25_scores),
                             ("hybrid_rrf", hybrid_scores), ("hybrid_evidence", evidence_scores)):
            results[name].append(ranked(scores))

    output = {"dataset": {"projects": 3, "queries": len(labels), "no_answer_queries": sum(bool(x.get("noAnswer")) for x in labels)},
              "warning": "dense_mock validates evaluation plumbing only; no real embedding quality claim is permitted", "variants": {}}
    for name, rankings in results.items():
        answer_cases = [(case, ranking) for case, ranking in zip(labels, rankings) if not case.get("noAnswer")]
        recalls5=[]; recalls10=[]; reciprocal=[]; ndcgs=[]; coverages=[]
        for case, ranking in answer_cases:
            relevant=set(case["relevant"]); recalls5.append(float(bool(relevant.intersection(ranking[:5])))); recalls10.append(float(bool(relevant.intersection(ranking[:10]))))
            reciprocal.append(next((1/index for index, path in enumerate(ranking[:10], 1) if path in relevant), 0)); ndcgs.append(ndcg(ranking, relevant)); coverages.append(len(relevant.intersection(ranking[:10]))/len(relevant))
        no_answer = [(case, ranking) for case, ranking in zip(labels, rankings) if case.get("noAnswer")]
        output["variants"][name] = {"recall_at_5": round(statistics.mean(recalls5), 4), "recall_at_10": round(statistics.mean(recalls10), 4),
            "mrr_at_10": round(statistics.mean(reciprocal), 4), "ndcg_at_10": round(statistics.mean(ndcgs), 4),
            "path_hit_rate": round(statistics.mean(recalls5), 4), "evidence_coverage": round(statistics.mean(coverages), 4),
            "no_answer_false_positive": round(statistics.mean(float(bool(ranking)) for _, ranking in no_answer), 4),
            "mean_latency_ms": round(statistics.mean(latencies[name]), 4), "p95_latency_ms": round(percentile(latencies[name], .95), 4)}
    json.dump(output, sys.stdout, ensure_ascii=False, indent=2); print()
    return 0


if __name__ == "__main__": raise SystemExit(main())

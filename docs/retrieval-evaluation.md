# Retrieval evaluation

## Reproducible fixture

The repository contains 3 entirely fictional projects and 36 fixed queries under `evaluation/`:

- `spring-orders`: Java/Spring-style code and YAML;
- `vue-incident-board`: Vue/TypeScript and Vite configuration;
- `ops-ledger`: SQL, Docker and operational YAML;
- 21 answerable queries and 15 explicit no-answer queries. The negatives include missing features that share real identifiers with existing code, such as JWT signature checks, deadline enforcement and focus management.

Run from the repository root:

```powershell
python scripts/evaluate-retrieval.py
```

The script reports Recall@5/10, MRR@10, NDCG@10, Path Hit Rate, Evidence Coverage, no-answer false-positive rate, mean latency and p95 for lexical, mock dense, BM25, RRF and evidence reranking. Labels are versioned and the script evaluates every query; there is no hidden-query exclusion.

## Local deterministic result (2026-11-23)

The local run processed all 36 queries. The previously referenced SQL fixture is now tracked so the evaluator sees the same corpus in clean checkouts; missing label paths now fail the run. These numbers describe only the synthetic fixture and deterministic process double:

| Variant | R@5 | R@10 | MRR@10 | NDCG@10 | Coverage | No-answer FP |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Lexical | 0.9524 | 0.9524 | 0.8254 | 0.8496 | 0.9286 | 0.8667 |
| Dense mock | 1.0000 | 1.0000 | 0.7889 | 0.8370 | 1.0000 | 1.0000 |
| BM25 | 0.9524 | 0.9524 | 0.8571 | 0.8734 | 0.9286 | 0.8667 |
| Hybrid + RRF | 1.0000 | 1.0000 | 0.7952 | 0.8517 | 1.0000 | 1.0000 |
| Hybrid + evidence rerank | 1.0000 | 1.0000 | 0.9063 | 0.9223 | 1.0000 | 1.0000 |

The mock dense vector is stable token feature hashing, not semantic-model quality evidence. In this script a false positive means returning **any candidate** for a no-answer query, not that production returned an answer. High candidate false-positive rates warrant testing a separate abstention decision on held-out examples. This baseline does not tune a threshold on these same labels or change the production path. In-memory timings are not service capacity.

## Real dense result

Real Dense quality evaluation was not executed: this workspace has no authorized paid embedding configuration available to the task. No real-Dense accuracy or latency claim is made. To produce a publishable result, set a lawful test key outside the repository, index the same fixtures with the configured model/dimension, save the unedited output and record model name, date, hardware, Milvus version and all failures. Do not commit the key.

CI runs the deterministic evaluator as a smoke test and separately starts real Milvus with a local deterministic embedding double. The Milvus test proves collection/schema, Dense, built-in BM25, hybrid RRF and tenant/generation isolation; it does not upgrade mock metrics into semantic quality claims.

# PMAI v7 Milvus Evidence-aware Hybrid RAG

## Status and scope

PMAI v7 keeps the modular monolith and adds an optional retrieval/indexing subsystem. MySQL remains the authority for users, project ownership, files, index state and generations. RabbitMQ transports durable business work. Milvus stores derived, rebuildable code chunks. The default retrieval mode remains `lexical`; this code has not been deployed to production.

## Data flow

```text
project file transaction
  -> pm_vector_index_task + pm_vector_index_outbox (same MySQL transaction)
  -> outbox relay -> pmai.index.v1 -> quorum queue
  -> fenced worker -> chunk -> redact -> embedding batch -> Milvus pending generation
  -> validate counts/source snapshot/searchability
  -> short MySQL transaction switches active_generation
  -> old generation cleanup

authenticated question
  -> MySQL ownership check + active_generation
  -> query embedding
  -> Milvus Dense COSINE + built-in BM25
  -> RRFRanker / rank-only RRF
  -> evidence-aware rerank + diversity
  -> canonical MySQL file hydration and hash check
  -> Q&A context with path and real line range
```

Milvus and RabbitMQ have deliberately different jobs. RabbitMQ owns delivery, retry and dead-letter routing for index work; it is not Milvus's internal messaging layer. Milvus owns retrieval data and indexes; it does not decide authorization, task state or which generation is visible.

## Shared Collection

The versioned Collection is `pmai_code_chunks_v1`. A Collection per project would make schema upgrades, index loading and operational cardinality grow with tenant count. A shared Collection permits one schema and bounded operational work while `user_id` acts as the partition key and every query uses server-built template filters for `user_id`, `project_id` and `active_generation`.

| Field | Purpose |
| --- | --- |
| `chunk_id` | physical primary key derived from stable logical chunk ID plus generation |
| `user_id` | partition key and tenant filter |
| `project_id`, `file_id`, `generation` | project/file/generation isolation |
| `file_path`, `language`, `chunk_type`, `symbol_name` | evidence metadata and rerank features |
| `start_line`, `end_line`, `content_hash` | canonical citation verification |
| `content` | original chunk returned as indexed snapshot fallback |
| `search_text` | redacted, token-enriched BM25 input |
| `dense_vector` | configured `FLOAT_VECTOR`, HNSW, COSINE |
| `sparse_vector` | BM25 Function output, sparse inverted index, BM25 metric |

Scalar indexes cover tenant/project/generation and common evidence fields. Collection creation asserts the configured dimension. The application never accepts a filter expression from the browser.

## Chunking and secrets

`EvidenceAwareCodeChunker` is deterministic heuristic chunking, not AST parsing. It recognizes structure and blank-line boundaries, applies line/character ceilings with overlap, and falls back to fixed windows. It supports Java, TypeScript/JavaScript, Vue, Python, SQL, YAML, JSON, Markdown, XML, properties, Dockerfile and shell.

The logical chunk hash includes project, file/path, content hash, start/end lines, chunker version and embedding model. Every chunk keeps original content and line numbers. `search_text` enriches path, symbol, CamelCase and snake_case tokens. Existing ZIP/index policies exclude dependency/build/binary/secret paths and oversized files. Before an external embedding call, likely credentials are replaced in the embedding/search representation; raw secrets are neither logged nor used as metric labels.

## Generation consistency

When generation G1 is active, a content change reserves G2 as pending. Workers may write only G2. They validate insert count, file count, basic searchability and the source snapshot. A short transaction first completes the task under its worker/execution-version fence and then switches `active_generation` to G2. A lost lease, partial insert or process crash cannot make G2 visible. Queries always read the generation from MySQL. G1 is deleted only after the switch.

One nullable unique `active_key` closes duplicate rebuild races. Lease claim, heartbeat and terminal updates use conditional SQL and `execution_version`; a resumed stale worker cannot commit. Consumer delivery is at least once, so task claiming and generation-specific upsert keys provide idempotence.

## Retrieval and explanations

Dense retrieval handles semantic phrasing; BM25 retains exact paths, class names, config keys and technical tokens. The first fusion is reciprocal-rank fusion because COSINE and BM25 raw scores are not directly comparable. `PMAI_RETRIEVAL_RRF_K` defaults to 60 and is configurable; it is an operational default, not a claim of optimal tuning.

Evidence-aware reranking then considers path/symbol/term matches, implementation evidence types, content completeness and repeated-file penalties. It prevents documentation from automatically outranking code. The response can expose dense/BM25 ranks and scores, RRF rank, evidence boost, final rank, matched terms and explanation to internal clients; the regular UI shows only useful evidence concepts, not Milvus internals.

Canonical evidence is re-read from `pm_project_file`. If its hash still matches, citations use the current source lines. Otherwise the indexed snapshot is used while the new generation is prepared.

## Modes and failure boundary

- `lexical`: existing MySQL keyword retrieval only.
- `shadow`: runs both paths, records low-cardinality latency/delta telemetry, returns lexical evidence.
- `hybrid`: returns Milvus hybrid evidence.

Milvus disabled/unready, missing generation, embedding errors, dimension errors, timeout, empty unverifiable results and Milvus exceptions all return `fallback/lexical`; Q&A does not return 500 for these retrieval failures. No vector-specific credit is charged. Logs record only exception classes or sanitized reasons.

Project deletion immediately removes the MySQL authority checked by every API call and marks vector state for asynchronous cleanup. Residual chunks remain unreachable because queries require an owned project and server-built tenant/project/generation filters.

## Final consistency and known limits

The index is eventually consistent with project content; a successful content transaction does not imply an immediately active new generation. The old complete generation remains queryable during rebuild. Local dispatch is a single-process compatibility mode; production reliability requires Rabbit dispatch. Milvus Standalone is not a highly available cluster. The heuristic chunker is not syntax-perfect. Real semantic quality still requires evaluation with a lawful production-equivalent embedding model; CI feature-hash vectors validate plumbing only.

# PMAI v7 Milvus Evidence-aware Hybrid RAG

## Status and scope

PMAI v7 keeps the modular monolith and adds an optional retrieval/indexing subsystem. MySQL remains the authority for users, project ownership, files, index state and generations. RabbitMQ transports durable business work. Milvus stores derived, rebuildable code chunks. The v7 application and V1–V6 migrations were deployed to production on 2026-11-20. Production remains in `lexical` mode; Milvus, Embedding and RabbitMQ dispatch are implemented and integration-tested but are not enabled in production.

## Data flow

```text
project file transaction
  -> increment source_revision; reserve target_revision
  -> pm_vector_index_task + pm_vector_index_outbox (same MySQL transaction)
  -> outbox relay -> pmai.index.v1 -> quorum queue
  -> fenced worker -> unique build_id -> chunk -> redact -> embedding batch -> Milvus pending generation/build
  -> validate counts/source snapshot/searchability
  -> short fenced MySQL transaction checks source_revision == target_revision
  -> switches active_generation + active_build_id; exact old generation/build cleanup

authenticated question
  -> MySQL ownership check + active_generation/build
  -> query embedding
  -> Milvus Dense COSINE + built-in BM25
  -> server-side RRFRanker (authoritative initial candidate order)
  -> canonical MySQL file hydration and hash check
  -> evidence-aware business rerank + diversity
  -> Q&A context with path and real line range
```

Milvus and RabbitMQ have deliberately different jobs. RabbitMQ owns delivery, retry and dead-letter routing for index work; it is not Milvus's internal messaging layer. Milvus owns retrieval data and indexes; it does not decide authorization, task state or which generation is visible.

## Shared Collection

The versioned Collection is `pmai_code_chunks_v1`. A Collection per project would make schema upgrades, index loading and operational cardinality grow with tenant count. A shared Collection permits one schema and bounded operational work while `user_id` acts as the partition key and every query uses server-built template filters for `user_id`, `project_id` and `active_generation`.

| Field | Purpose |
| --- | --- |
| `chunk_id` | physical primary key derived from stable logical chunk ID, generation and build |
| `user_id` | partition key and tenant filter |
| `project_id`, `file_id`, `generation`, `build_id` | project/file/generation/build-attempt isolation |
| `file_path`, `language`, `chunk_type`, `symbol_name` | evidence metadata and rerank features |
| `start_line`, `end_line`, `content_hash` | canonical citation verification |
| `content` | redacted preview used only as a rerank hint; never authoritative response evidence |
| `search_text` | redacted, token-enriched BM25 input |
| `dense_vector` | configured `FLOAT_VECTOR`, HNSW, COSINE |
| `sparse_vector` | BM25 Function output, sparse inverted index, BM25 metric |

Scalar indexes cover tenant/project/generation/build and common evidence fields. Every search filter contains server-built `user_id`, `project_id`, `generation` and `build_id` values. Collection creation asserts the configured dimension. The application never accepts a filter expression from the browser.

## Chunking and secrets

`EvidenceAwareCodeChunker` is deterministic heuristic chunking, not AST parsing. It recognizes structure and blank-line boundaries, applies line/character ceilings with overlap, and falls back to fixed windows. It supports Java, TypeScript/JavaScript, Vue, Python, SQL, YAML, JSON, Markdown, XML, properties, Dockerfile and shell.

The logical chunk hash includes project, file/path, canonical content hash, start/end lines, chunker version and embedding model. `ProjectContentHasher` normalizes CRLF/CR to LF, hashes UTF-8 bytes with SHA-256, and is shared by chunking, snapshot validation and retrieval hydration. `search_text` enriches path, symbol, CamelCase and snake_case tokens. Existing ZIP/index policies exclude dependency/build/binary/secret paths and oversized files. Before an external embedding call or Milvus preview write, likely credentials are replaced; raw secrets are neither logged nor used as metric labels.

## Generation consistency

Each successful logical content change increments `source_revision` in the file transaction; a ZIP batch increments once. A task captures `target_revision`. When generation/build G1/B1 is active, a change reserves the next generation and persists `reschedule_required` if work is already active. Workers validate insert count, file count, searchability and source snapshot. A short transaction requires matching worker, execution version, generation, build and `source_revision == target_revision` before switching both active pointers. A superseded/failed task creates the necessary successor task and durable Outbox in the same transaction, so the latest change is not lost.

One nullable unique `active_key` closes duplicate rebuild races. Every successful claim gets a non-reusable `build_id`; lease claim, heartbeat and terminal updates require worker, execution version and build. A resumed stale worker can neither commit nor delete the recovery worker's rows because cleanup is exact generation/build. Consumer delivery is at least once, so task claiming and build-specific upsert keys provide idempotence.

## Retrieval and explanations

Dense retrieval handles semantic phrasing; BM25 retains exact paths, class names, config keys and technical tokens. Milvus `HybridSearch` plus `RRFRanker` is the authoritative first fusion because COSINE and BM25 raw scores are not directly comparable. `PMAI_RETRIEVAL_RRF_K` defaults to 60 and is configurable; it is an operational default, not a claim of optimal tuning. Optional `PMAI_RETRIEVAL_PER_PATH_DIAGNOSTICS_ENABLED=true` performs extra Dense/BM25 single-path queries to attach diagnostic ranks by chunk ID without changing the server candidate set or initial RRF order; it is off by default to avoid three-query latency.

Evidence-aware reranking then considers path/symbol/term matches, implementation evidence types, content completeness and repeated-file penalties. It prevents documentation from automatically outranking code. The response can expose dense/BM25 ranks and scores, RRF rank, evidence boost, final rank, matched terms and explanation to internal clients; the regular UI shows only useful evidence concepts, not Milvus internals.

Canonical evidence is re-read from `pm_project_file` before business reranking. Only hash-matching candidates continue, and their snippet/path come from normalized current MySQL content. Missing or mismatched candidates increment a low-cardinality stale-evidence counter and are discarded; Milvus preview content is never returned. If every candidate is invalid, retrieval safely falls back to lexical.

## Modes and failure boundary

- `lexical`: existing MySQL keyword retrieval only.
- `shadow`: runs both paths, records low-cardinality latency/delta telemetry, returns lexical evidence.
- `hybrid`: returns Milvus hybrid evidence.

Milvus disabled/unready, missing generation, embedding errors, dimension errors, timeout, empty unverifiable results and Milvus exceptions all return `fallback/lexical`; Q&A does not return 500 for these retrieval failures. No vector-specific credit is charged. Logs record only exception classes or sanitized reasons.

Project deletion immediately removes the MySQL authority checked by every API call and, in the same transaction, persists a cleanup task plus Outbox message containing stable user/project identity. Rabbit mode uses confirm/manual ACK/retry/DLQ; local compatibility mode also reclaims committed Outbox rows on a scheduled scan after restart. Cleanup is idempotent `deleteProject`, and residual chunks remain unreachable while cleanup is pending.

## Final consistency and known limits

The index is eventually consistent with project content; a successful content transaction does not imply an immediately active new generation. The old complete generation/build remains queryable during rebuild. Local dispatch durably resumes its Outbox after restart but remains a single-process compatibility mode; production delivery isolation and DLQ operations require Rabbit dispatch. Milvus Standalone is not a highly available cluster. The heuristic chunker is not syntax-perfect. Real semantic quality still requires evaluation with a lawful production-equivalent embedding model; CI feature-hash vectors validate plumbing only.

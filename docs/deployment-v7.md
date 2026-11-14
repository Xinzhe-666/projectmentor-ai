# PMAI v7 deployment and rollback runbook

This is preparation only. The branch must not be merged or deployed by this task.

## Preconditions and stop conditions

Require green backend/frontend/repository/database/Rabbit/Milvus CI, a MySQL backup restore drill, generated secrets outside Git, an isolated capacity run, and enough memory for MySQL, RabbitMQ, backend, etcd, MinIO and Milvus. Stop if total memory leaves unsafe headroom, swap/major GC grows, OOM Killer activity appears, migration preflight fails, or rollback artifacts are missing.

## Staged rollout

1. Back up MySQL and verify the backup file; record current backend/frontend image digests and Git rollback SHA.
2. Start pinned Milvus Standalone with persistent volumes.
3. Enable authentication, run the one-time RBAC initializer, rotate root and verify the least-privilege app login.
4. Start RabbitMQ and verify quorum queues, publisher confirm/return and DLQ policy.
5. Execute immutable V6 through Flyway; validate all four new tables/indexes and retain V1–V5 unchanged.
6. Start the backend with `PMAI_RETRIEVAL_MODE=lexical`; keep index consumer concurrency bounded.
7. Index existing projects in controlled batches; observe outbox, queue, retries, heap, DB pool and Milvus memory.
8. Change to `shadow`; compare latency/fallback/result-delta metrics without changing user answers.
9. After an observation window and quality review, change to `hybrid` for a controlled cohort/environment.
10. Deploy the frontend and run desktop/mobile, ownership, generation, delete and Q&A fallback smoke tests.

Never make the first v7 startup mandatory-hybrid.

## Rollback

1. Set `PMAI_RETRIEVAL_MODE=lexical` and restart/roll configuration; Q&A continues through the established path.
2. Stop index consumers/relay if they are contributing load. Preserve task/outbox rows.
3. Roll backend and frontend images to recorded digests.
4. Keep V6 tables: the migration is forward-compatible and must not be manually undone during incident response.
5. Keep MySQL, RabbitMQ and all Milvus/etcd/MinIO volumes. Do not use `down -v` in production.
6. Diagnose/rebuild derived generations, then re-enter through lexical -> shadow -> hybrid.

Rollback success means core auth/project/report/analysis/credits/Q&A lexical paths are healthy, no new vector work executes, and no data volume was deleted.

## Post-rollout evidence

Archive the migration history, image digests, RBAC check, queue/outbox drain, generation switch sample, isolation tests, sanitized metrics, mobile screenshots and capacity report. Known limitations remain: Standalone is not HA, heuristic chunking is not AST parsing, local dispatch is not durable across processes, and real Dense quality is model/dataset dependent.

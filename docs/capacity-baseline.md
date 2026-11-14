# PMAI v7 capacity baseline

## Result status

No full container capacity result is claimed for this change. On 2026-11-14 the local Docker engine was unavailable (`dockerDesktopLinuxEngine` pipe missing), so real Rabbit/Milvus concurrency, chunks/s, JVM/GC and container-memory measurements could not be executed safely. This is a release evidence gap, not a fabricated pass. The independent CI jobs provide functional integration gates; they are not a production capacity benchmark.

The completed local evidence is:

- backend unit suite: 158/158 passed before the additional four consistency/fallback tests; final suite must be rerun before push;
- deterministic retrieval fixture: 3 projects / 24 queries completed;
- frontend production build and all Compose configuration combinations validated earlier in this branch;
- localhost-only smoke harness added at `scripts/v7-capacity-smoke.ps1`.

## Isolated benchmark contract

Use Compose project `pmai-v7-perf`, never production and never `projectmentorai.com`. The smoke script rejects non-loopback hosts:

```powershell
docker compose -p pmai-v7-perf -f docker-compose.yml -f docker-compose.rabbit.yml -f docker-compose.milvus.yml up -d
pwsh scripts/v7-capacity-smoke.ps1 -Requests 500 -Concurrency 16
pwsh scripts/v7-capacity-smoke.ps1 -Requests 200 -Concurrency 8 -ProjectId 101 -BearerToken $env:PMAI_PERF_TOKEN -ExerciseDuplicateRebuild
```

Use fictional fixture users/projects only. Record CPU count, memory, disk, Java version, image digests, dataset files/chunks, embedding double/model and configuration alongside raw output.

## Required full-run matrix

1. Multiple project indexes with duplicate rebuild clicks; verify one active task per project.
2. Embedding batches at configured size and injected 429, 5xx and timeout responses.
3. Index queue accumulation and drain while analysis traffic runs concurrently.
4. Milvus search plus Q&A at stepped concurrency.
5. Stop/restart Milvus; measure lexical fallback and recovery time.
6. Pause a worker beyond its lease; verify fencing rejects its terminal commit.
7. Observe G1 while G2 is partial, then atomic switch and old-generation cleanup.
8. Delete a project while residual chunks remain; every API query must fail ownership/existence checks.

Capture index chunks/s, embedding batch latency, project index time, query p50/p95/p99, hybrid p95, Rabbit ready/unacked, maximum outbox backlog, recovery time, heap/GC, Hikari active/waiting, Milvus memory and failure/retry/DLQ totals.

## Release blockers

Any cross-user leak, visible partial generation, duplicate active task, post-delete retrieval, Q&A 500 during Milvus failure, unbounded memory, healthy DLQ count above zero, stuck outbox, or credits/report/analysis regression blocks rollout. A real baseline must be attached before production hybrid enablement; this PR prepares the harness but does not satisfy that deployment gate by itself.

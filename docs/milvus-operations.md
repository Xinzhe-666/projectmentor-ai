# Milvus Standalone operations

## Versions and exposure

- Milvus Server: `milvusdb/milvus:v2.6.26`
- Java SDK: `io.milvus:milvus-sdk-java:2.6.27`
- etcd: `quay.io/coreos/etcd:v3.5.25`
- MinIO: `milvusdb/minio:RELEASE.2024-12-18T13-15-44Z`

`docker-compose.milvus.yml` follows the Standalone layout and uses named volumes. gRPC 19530, health/metrics 9091 and MinIO console 9001 bind to localhost only for controlled administration. Production firewalls must not expose them publicly; the backend uses `http://milvus-standalone:19530` on the Compose network.

## One-time authenticated initialization

1. Put strong, unique placeholder replacements in an untracked `.env`: MinIO credentials, `MILVUS_APP_USERNAME`, `MILVUS_APP_PASSWORD`, and a new root password used only during initialization.
2. Start only Milvus dependencies and wait for `/healthz`.
3. Run `scripts/init-milvus-rbac.ps1` from an administrative workstation. The script invokes `MilvusRbacInitializer`; root credentials are used for this one controlled operation, never by normal application startup.
4. The initializer creates the database/Collection, application role and user, grants the collection read/write plus required describe/load privileges, and rotates the root password. It does not print secrets.
5. Configure the backend `MILVUS_TOKEN` indirectly as `${MILVUS_APP_USERNAME}:${MILVUS_APP_PASSWORD}` through Compose.

Authentication is enabled in `deploy/milvus/milvus.yaml`. Never leave `root:Milvus` active after initialization, never commit `.env`, and do not grant the application user root/admin privileges.

## Start and validate

```powershell
docker compose -f docker-compose.yml -f docker-compose.rabbit.yml -f docker-compose.milvus.yml config --quiet
docker compose -f docker-compose.yml -f docker-compose.rabbit.yml -f docker-compose.milvus.yml up -d milvus-standalone rabbitmq
curl.exe --fail http://127.0.0.1:9091/healthz
```

Keep `PMAI_RETRIEVAL_MODE=lexical` during initial bring-up. Confirm Actuator metrics, index backlog and successful test-project indexing before using `shadow` and then `hybrid`.

## Backup, recovery and upgrades

Milvus data, MinIO objects and etcd metadata are a coordinated derived-data set. Application recovery can always rebuild them from MySQL, so do not restore only one volume and assume consistency. Before a Milvus upgrade, record exact images, stop writers, take platform-appropriate snapshots of all three named volumes, and keep the current application image. Validate vendor upgrade compatibility in an isolated Compose project.

If Milvus is unavailable, immediately set retrieval to `lexical` and optionally stop the index consumer. Do not delete volumes. Restore service, verify authentication and Collection schema, resume consumers, observe retry/outbox drain, then use `shadow` before `hybrid`. A dimension/model change requires a new Collection schema/version and full rebuild; do not mutate the live vector dimension in place.

## Operational checks

Watch container memory, disk, `/healthz`, Milvus search/insert errors, index retry/DLQ, task leases, outbox backlog and generation cleanup. Stop rollout if Milvus memory threatens MySQL/Rabbit/backend survival, if outbox cannot drain, if healthy traffic creates DLQ entries, or if any tenant/generation isolation test fails. Sanitized logs may include task/correlation IDs but never tokens, embedding keys or raw source.

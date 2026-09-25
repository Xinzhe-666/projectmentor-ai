# RabbitMQ 分析任务管线运维说明

## 启用方式

默认部署保持 `ANALYSIS_DISPATCH_MODE=local`，无需 RabbitMQ。需要可靠跨实例调度时，先复制 `.env.example` 为本机 `.env`，替换 MySQL、JWT 和 RabbitMQ 占位凭据，再运行：

```bash
docker compose -f docker-compose.yml -f docker-compose.rabbit.yml up -d
```

Fast 构建可叠加 `docker-compose.fast.yml`。提交或排障前可执行：

```bash
docker compose config --quiet
docker compose -f docker-compose.yml -f docker-compose.fast.yml config --quiet
docker compose -f docker-compose.yml -f docker-compose.rabbit.yml config --quiet
docker compose -f docker-compose.yml -f docker-compose.fast.yml -f docker-compose.rabbit.yml config --quiet
```

RabbitMQ Management UI 默认只绑定宿主机 `127.0.0.1:15672`，AMQP 默认只绑定 `127.0.0.1:5672`。不要把示例占位凭据用于共享或生产环境。

## 默认拓扑和策略

- Main exchange/queue：`pmai.analysis.v1` / `pmai.analysis.execute.v1`
- Retry exchange：`pmai.analysis.retry.v1`
- DLX/DLQ：`pmai.analysis.dlx.v1` / `pmai.analysis.dead.v1`
- Queue type：`quorum`
- Retry TTL：10 秒、60 秒、300 秒
- 最大执行尝试：4
- Publisher confirm timeout：5 秒
- Consumer：手动 ACK，prefetch 1，默认并发 1–4
- 执行租约/心跳：120 秒 / 30 秒

所有值都可通过 `.env.example` 中的变量覆盖。心跳必须明显小于租约，Consumer 最大并发不得小于最小并发，TTL、超时、批量大小和尝试次数必须为正；Rabbit 模式缺少连接信息时应用会启动失败，Local 模式不会因此连接 Broker。

## 故障判断

MySQL 中的任务、租约、Outbox 和额度流水是事实来源。RabbitMQ 管理页面中的 ready/unacked 数量只说明消息传输状态，不能代替业务终态。

- Outbox 长时间为 `NEW`：检查 Broker 连通性、exchange/binding、Return/NACK 和下次尝试时间。
- Outbox 为 `CLAIMED`：Relay 可能仍在等待 Confirm；超过 claim lease 后可由其他实例重新领取。
- Outbox 为 `FAILED`：发布已超过上限，记录会保留，尚未执行的任务应进入失败终态。
- 任务长期 `RUNNING`：检查 heartbeat；租约过期后 Recovery Scanner 会恢复或终结。
- DLQ 有消息：按 message/task/correlation ID 关联结构化日志和数据库记录，不要直接批量重放。确认根因和幂等状态后，再设计受控重放流程。

Actuator 的 `metrics` 端点可查看 `pmai.analysis.*` 指标。taskId、userId、projectId 和 messageId 不作为指标标签，只用于日志关联。

## 可用性边界

Compose 使用固定 RabbitMQ 4.1 管理版镜像和持久化 volume。单节点 quorum queue 仅提供持久化及一致的队列语义，不构成高可用集群。生产高可用至少需要三个 RabbitMQ 节点，并需要独立验证磁盘水位、网络分区、滚动升级、备份和恢复。本阶段没有执行生产部署或正式容量压测。

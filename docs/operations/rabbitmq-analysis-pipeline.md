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
- Dead-letter：Main/Retry 使用 `at-least-once`，overflow 为 `reject-publish`
- Broker delivery limit：20（可通过 `ANALYSIS_RABBIT_DELIVERY_LIMIT` 调整，范围 1–1000）
- Retry TTL：10 秒、60 秒、300 秒
- 最大执行尝试：4
- Publisher confirm timeout：5 秒
- Consumer：手动 ACK，prefetch 1，默认并发 1–4
- 执行租约/心跳：120 秒 / 30 秒

所有值都可通过 `.env.example` 中的变量覆盖。Rabbit 模式只接受 quorum queue；配置为 classic 会快速启动失败，不能把 classic 描述为具有相同 dead-letter 可靠性。心跳必须明显小于租约，Consumer 最大并发不得小于最小并发，delivery limit、TTL、超时、批量大小和尝试次数必须为正；Rabbit 模式缺少连接信息时应用会启动失败，Local 模式不会因此连接 Broker。

Main Queue 和每个 Retry Queue 都设置 `x-dead-letter-strategy=at-least-once`、`x-overflow=reject-publish`、`x-delivery-limit` 以及明确的 DLX/routing key。DLQ 自身没有 DLX。修改已有 Queue 的这些 immutable arguments 会触发 RabbitMQ `PRECONDITION_FAILED`，不能直接原地覆盖：应停止生产者/消费者、确认并受控处理积压、删除并按新参数重建目标队列，再恢复流量；不要删除 RabbitMQ volume。本功能尚未部署，因此本次不存在生产队列迁移。

## 故障判断

MySQL 中的任务、租约、Outbox 和额度流水是事实来源。RabbitMQ 管理页面中的 ready/unacked 数量只说明消息传输状态，不能代替业务终态。

- Outbox 长时间为 `NEW`：检查 Broker 连通性、exchange/binding、Return/NACK 和下次尝试时间。
- Outbox 为 `CLAIMED`：Relay 可能仍在等待 Confirm；每条发布前会条件续租，超过 claim lease 后可由其他实例重新领取，旧 owner 续租失败后不会发布。
- Outbox 为 `FAILED`：发布已超过上限，记录会保留，尚未执行的任务应进入失败终态。
- 任务长期 `RUNNING`：检查 heartbeat；租约过期后 Recovery Scanner 会恢复或终结。
- DLQ 有消息：按 message/task/correlation ID 关联结构化日志和数据库记录，不要直接批量重放。确认根因和幂等状态后，再设计受控重放流程。

数据库异常时不得 ACK 成功。该路径使用 `basic.reject(requeue=true)`，因为 RabbitMQ 4.1 的 quorum delivery count 会对 AMQP 0-9-1 reject 计数，而普通 `basic.nack` 不提供同样的计数保证。若数据库在任务 claim 前不可用，Consumer 会有限 requeue；超出 quorum delivery limit 后消息进入 DLQ，任务仍保持 `PENDING`，数据库恢复后需要先核对任务/Outbox/DLQ 再受控重放。若数据库在 claim 提交后不可用，消息同样受 delivery limit 保护，而任务可能暂时保持 `RUNNING`；数据库恢复且租约过期后，Recovery Scanner 会依据 execution version 恢复或终结任务。数据库不可用期间 Scanner 也无法推进，RabbitMQ 不能替代 MySQL 业务状态。

AI 瞬时错误在未达上限时进入 Retry Queue，不退款也不保存半成品。AI disabled、Key 缺失、HTTP 400/401/403/404 等永久错误，以及最后一次仍失败的 408/429/5xx/网络错误，会生成规则降级报告；报告、项目 `FINISHED`、任务 `SUCCESS`、活动键释放和任务级退款同事务提交，不进入 DLQ。规则扫描、权限、额度、非法状态或最终持久化失败不伪造降级成功。

Actuator 的 `metrics` 端点可查看 `pmai.analysis.*` 指标。taskId、userId、projectId 和 messageId 不作为指标标签，只用于日志关联。

## 可用性边界

Compose 使用固定 RabbitMQ 4.1 管理版镜像和持久化 volume。单节点 quorum queue 仅提供持久化及一致的队列语义，不构成高可用集群。生产高可用至少需要三个 RabbitMQ 节点，并需要独立验证磁盘水位、网络分区、滚动升级、备份和恢复。本阶段没有执行生产部署或正式容量压测。

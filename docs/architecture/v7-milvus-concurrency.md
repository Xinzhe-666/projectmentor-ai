# PMAI v7 Phase 0：高并发安全基础

## 范围与结论

Phase 0 只加固现有模块化单体中的审计主链路，不引入 RabbitMQ、Milvus、微服务或 Kubernetes。本阶段的目标是关闭已知的一致性窗口、缩短数据库事务、让本地执行器饱和时任务可靠进入终态，并为后续演进保留清晰边界。

任何“可支持多少并发”“吞吐提升多少”的结论都必须来自固定环境、固定数据集和可复现脚本的压测报告。本阶段没有这样的数据，因此不作性能容量承诺。

## 当前异步任务基线

审计入口先在 MySQL 的 `pm_analysis_task` 中创建任务，再将任务提交给 Spring `ThreadPoolTaskExecutor`。执行线程通过 `AnalysisTaskAsyncExecutor` 更新 `PENDING`、`RUNNING`、`SUCCESS` 或 `FAILED` 状态，并调用 `AnalysisReportService` 完成规则扫描、Claim-Evidence 分析和可选的 LLM 审计。

MySQL 是任务状态的真实来源。Redis 只保存短期进度缓存；Redis 不可用、缓存过期或缓存内容滞后，都不能改变 MySQL 中的任务状态，也不能让审计任务失败。

执行器采用有界队列与 `AbortPolicy`。线程数和队列容量通过 `projectmentor.analysis.executor` 配置，默认保持 `core=2`、`max=4`、`queue=50`，在取得压测数据前不扩大默认并发。

## 已处理的风险

### 数据库长事务

此前 `AnalysisReportService.generateReport` 的事务覆盖数据库查询、规则扫描、Claim-Evidence 分析、额度处理、远程 LLM 请求和最终保存。远程请求期间占用连接会在高并发时放大连接池耗尽风险。

现在规则扫描、Claim-Evidence、未来的 Embedding 预留处理和远程 LLM 调用均在事务外执行。只有“插入报告”和“将项目标记为 `FINISHED`”由 `AnalysisReportPersistenceService` 的短事务统一提交；任一步失败都会回滚该短事务。`enhanceClaimEvidence` 的远程 LLM 请求同样不再被长事务包围，单条 `updateById` 保持数据库自身的原子更新。

额度仍在 AI 调用前扣除。AI 失败、报告保存失败或项目状态保存失败时返还，单次调用路径使用本地退款状态避免重复退款。额度扣除和返还各自使用 `REQUIRES_NEW` 短事务，保留可审计流水。

所有基于旧余额计算新余额的操作都会先通过 `SELECT ... FOR UPDATE` 锁定该用户的 `pm_user_plan` 行。扣款、退款、管理员发放和管理员扣除均在锁内完成余额校验、before/after 计算、余额更新和流水插入，从而避免同一用户跨项目、跨实例并发时发生丢失更新。首次创建 Plan 仍由 `uk_user_id` 提供最终唯一性保护；唯一键竞争后会重新读取并锁定胜出的 Plan，不向 API 暴露数据库约束信息。注册赠送与新用户、Plan、赠送流水处于同一事务，并由用户唯一约束先关闭重复注册窗口。

### 同一项目重复提交

仅靠“先查询再插入”会留下并发窗口。V4 为 `pm_analysis_task.active_key` 建立单列唯一索引。活动的完整审计使用 `FULL_ANALYSIS:{projectId}`；`PENDING`、`RUNNING` 保持该值，`SUCCESS`、`FAILED` 清空为 `NULL`。MySQL 允许唯一索引中存在多条 `NULL`，因此历史终态任务可以共存，迁移也不会改写历史数据。

服务层先返回已有活动任务；两个请求同时通过预查询时，唯一索引负责关闭最终竞争窗口。失败方捕获 `DuplicateKeyException` 后重新读取胜出任务并返回。若约束冲突后仍找不到活动任务，则只返回稳定业务错误，不暴露 SQL、索引名或堆栈。

### 执行器拒绝

有界线程池饱和时，`AbortPolicy` 通过 Spring 的 `TaskRejectedException` 明确拒绝。服务将已经插入的任务更新为 `FAILED`、进度设为 100、记录用户提示和内部失败原因、设置完成时间并清空 `active_key`，不会遗留永久 `PENDING`。不使用无界队列，也不使用会让 Web 请求线程执行长耗时审计的 `CallerRunsPolicy`。

## Phase 1：RabbitMQ 可靠分析任务管线

Phase 1 在模块化单体内增加可渐进启用的可靠调度层。`projectmentor.analysis.dispatch-mode` 默认为 `local`，因此原有部署不需要 RabbitMQ；切换为 `rabbit` 后，API 不再提交本地线程池，而是在创建任务的同一个 MySQL 短事务中写入初始 Outbox。两种模式最终都调用同一个同步 `AnalysisTaskProcessor`，Rabbit Consumer 不再嵌套调用 `@Async`。

### Transactional Outbox 与发布边界

`pm_analysis_outbox` 是待发布事件的事实来源。API 事务要么同时提交 `pm_analysis_task` 与初始事件，要么同时回滚，不在数据库事务内等待网络。Relay 使用 `SELECT ... FOR UPDATE SKIP LOCKED` 短事务批量领取事件，写入带过期时间的唯一 claim owner，提交后才调用 RabbitMQ。每一条事件正式发布前都会按 `id + CLAIMED + claim_owner` 条件续租；续租失败表示所有权已经被其他实例接管，旧 Relay 必须跳过。Confirm 后的结果更新仍校验 owner，因此慢批次后半段不会由过期 owner 继续发送。

发布使用持久化消息、durable direct exchange/queue、`mandatory=true`、correlated Publisher Confirm 和 Publisher Return。只有 Confirm ACK 且没有 Return 才把 Outbox 标记为 `PUBLISHED`。NACK、Return、Confirm 超时和连接异常会记录清理后的原因，并按有上限的指数退避重新调度；超过发布上限时保留 `FAILED` Outbox，并安全终结仍未开始的任务。Confirm 只表示 Broker 接收并路由了消息，不表示 Consumer 已处理成功。

Broker 可能已接收消息，而进程在写回 `PUBLISHED` 前崩溃，因此发布语义是至少一次。消息只携带 message/task/project/user/correlation 定位信息、协议版本、尝试序号和创建时间，不携带源码、README、Prompt、JWT、邮箱或密钥。Consumer 会用 MySQL 重新校验任务、项目和用户关系，不信任消息中的权限或状态。

### 消费、租约与 fencing token

Consumer 使用手动 ACK，MySQL 是执行权和任务状态的事实来源。领取不是“先查再改”，而是一条条件 `UPDATE`：只有 `PENDING` 或租约已过期的 `RUNNING` 任务可以进入执行，同时递增 `execution_attempt` 与 `execution_version`，写入 worker、message ID、数据库时间计算的租约和心跳。

心跳、进度、重试转换、失败转换和最终报告提交都校验 worker ID 与 execution version。报告插入、项目 `FINISHED`、任务 `SUCCESS`、report ID 和 `active_key` 释放位于同一短事务；`pm_analysis_report.task_id` 唯一索引提供数据库级最终幂等。旧 Worker 即使在租约过期后恢复，也无法用旧 fencing token 写报告或覆盖新 Worker。

ACK 决策如下：

| 场景 | 数据库动作 | Broker 动作 |
| --- | --- | --- |
| 成功提交终态 | 原子提交报告与 `SUCCESS` | ACK |
| 已是 `SUCCESS`/`FAILED` 的重复消息 | 不再调用 AI；必要时补做幂等退款 | ACK |
| 有效租约正在执行的重复消息 | 不启动第二个 Worker | ACK |
| 永久 AI 错误，或瞬时 AI 错误已到最后一次 | fenced 保存规则降级报告、项目和任务 `SUCCESS`、同事务幂等退款 | ACK，不进入 DLQ |
| 非 AI 的不可重试业务错误 | fenced `FAILED`、释放活动键、幂等退款、写死信 Outbox | ACK |
| 可重试错误且未超限 | 任务回到 `PENDING`，同事务写入 Retry Outbox | ACK |
| 未知协议或无效消息 | 不执行任务 | Reject 到 DLQ |
| 消费结果无法持久化 | 不确认数据库结果 | Reject/requeue；delivery limit 后进入 DLQ |

### 有限重试、DLQ 与恢复

默认 Retry TTL 层级为 10 秒、60 秒、300 秒，最大执行尝试次数为 4；每次重试生成新的 message ID，保留 task/correlation identity。AI 连接/读取超时、HTTP 408/429/5xx 和明确临时网络错误可重试；AI disabled、Key 缺失、HTTP 400/401/403/404、模型/参数错误和无效返回结构直接生成规则降级报告。瞬时 AI 错误到最后一次也生成降级报告；报告明确标注 AI 不可用和额度已返还。规则报告、项目 `FINISHED`、任务 `SUCCESS`、`active_key` 释放与任务级退款在同一 fenced 短事务内，任一步失败全部回滚。项目/权限/额度/状态、规则扫描或最终持久化错误仍按非 AI 失败处理。Spring AMQP 自动业务重试关闭，避免和持久化重试叠加。

Worker 心跳停止后，Recovery Scanner 通过数据库行锁和条件版本更新领取过期任务。未超限时，任务回到可调度状态并在同一事务写 Retry Outbox；超限时进入 `FAILED`、清空 `active_key`，并写入死信诊断事件。Rabbit 模式正式要求 quorum queue，classic 配置会启动失败。Main Queue 和每个 Retry Queue 显式使用 `x-dead-letter-strategy=at-least-once`、`x-overflow=reject-publish` 和有限 `x-delivery-limit`；Main 超限进入 DLQ，Retry Queue 在 TTL 后可靠转回 Main，DLQ 自身不再配置 DLX。开发 Compose 的单节点 RabbitMQ 只提供持久化和一致队列语义，不等于高可用。未来生产高可用至少需要三节点 RabbitMQ，并应另行验证网络分区、磁盘告警和恢复流程。

### Credits 与可观测性

V5 为 `pm_credit_log.idempotency_key` 增加 nullable unique key。分析任务使用稳定的 `ANALYSIS_DEBIT:{taskId}` 和 `ANALYSIS_REFUND:{taskId}`：余额行先 `FOR UPDATE`，余额更新与流水写入同一事务，数据库唯一约束关闭重复投递竞争窗口；没有成功 debit 的任务不能退款，同一任务最多退款一次。现有管理员额度操作和同步报告接口继续使用原有兼容路径。

指标复用 Actuator/Micrometer，覆盖提交、Outbox 领取/发布/失败、消费/重复/重试/死信、租约恢复、活跃 Worker、Outbox backlog 和执行时长。指标标签只包含 mode/result 等低基数字段；task/user/project/message 标识只进入经过清理的结构化日志。

真实 RabbitMQ 集成门禁覆盖 Broker 协议和拓扑语义：Confirm/Return、持久化消息、物理 Channel manual ACK/redelivery、Retry TTL、有限 requeue 后 DLQ、at-least-once DLX 目标延迟创建后的恢复，以及 Broker unavailable 后 Outbox 恢复。Consumer 任务领取、重复投递、AI 分类、规则降级和 fenced 最终提交由协调器/服务单元测试与 MySQL 真实集成测试覆盖；当前不把这些测试描述为完整启动真实 `@RabbitListener` 的 AI 端到端测试。

## 尚未解决的风险

- ZIP 上传仍在请求线程同步解压和解析。大文件会占用请求线程、CPU、内存和磁盘 I/O，需在独立阶段异步化并加入资源配额。
- 当前项目 QA 会从 MySQL 读取大量文件内容并执行关键词扫描。项目规模和并发增长后，这会放大数据库读取、JVM 内存和 CPU 压力。
- 同步报告生成旧接口仍然存在；本阶段的活动任务唯一键保护异步 `startAnalysis` 主链路。后续应统一入口或为同步入口定义明确的并发策略。
- `InterviewService` 可能存在跨远程调用的事务边界风险。本阶段按范围不做大规模重构，应在后续专项审查。
- `local` 兼容模式仍是单 JVM 的有界内存队列，不具备跨进程持久投递；需要上述保证的环境必须显式启用 `rabbit`。
- 本阶段尚未实施 Milvus、Embedding、BM25、Reranker、ZIP 异步解析或正式容量压测，也不对吞吐或 RabbitMQ 集群高可用作承诺。

## 后续演进边界

### Phase 1：RabbitMQ 持久任务调度（本阶段已实现）

RabbitMQ 用于持久任务投递、消费确认、有限重试、死信与跨实例调度；数据库唯一键和 fencing token 仍是最终一致性防线，消息队列不替代业务幂等。

### Phase 2：异步上传和批量解析

将 ZIP 接收、病毒与压缩炸弹防护、解压、文件筛选、批量持久化和解析进度拆为可恢复任务，设置大小、文件数、解压比、时间和资源上限。

### Phase 3：Milvus Code RAG

Milvus 只承担代码片段向量检索，不成为任务状态、权限、额度或报告数据的真实来源。首版采用 Standalone，是因为当前仍为单体、数据规模和可用性目标尚未证明需要分布式集群；Standalone 运维面更小，便于先验证切片、Embedding、召回和重排质量。只有容量、可用性和恢复指标证明需要时，才评估集群形态。

### Phase 4：Prometheus/Grafana与Gatling压测

先建立执行器活跃线程、队列深度、拒绝数、任务等待/执行时长、数据库连接池、LLM 延迟与错误率等指标，再用 Gatling 在固定环境中验证重复点击、突发流量、队列饱和和下游超时。高并发结论必须引用测试配置、数据规模、原始结果和瓶颈分析。

## 为什么继续保持模块化单体

当前主要风险来自事务边界、幂等、队列持久性和同步重任务，而不是缺少服务拆分。模块化单体能用更低的部署、调试和数据一致性成本修正这些基础问题。过早拆成微服务会增加网络失败、分布式事务、观测和部署复杂度，却不会自动解决幂等或容量问题。待监控和压测证明存在独立扩缩容、故障隔离或团队边界需求后，再以数据决定拆分。

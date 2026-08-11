# 数据库迁移与 Schema 版本治理

V4.9-0 起，ProjectMentor AI 使用 Flyway 管理业务 schema。本文说明全新数据库、已有 legacy 数据库、备份恢复和后续 migration 的安全流程。

这套机制用于建立可重复的 schema 版本历史，不代表自动回滚、零停机迁移、完整 schema drift detection、企业级 DBA 平台或灾难恢复系统。

## 唯一真源与术语

业务 schema 的唯一真源是：

```text
backend/projectmentor-server/src/main/resources/db/migration/V*.sql
```

当前首个文件是：

```text
V1__baseline_schema.sql
```

V1 是一个 Flyway **versioned migration**，用于在全新空数据库中创建当前业务 schema。它表示项目接入 Flyway 时的 schema 基线，但不要与 Flyway 使用 `B` 前缀的 baseline migration 混淆。

对已有非空数据库执行 baseline 是另一种操作：Flyway 创建 `flyway_schema_history` 并记录 version 1，所有不高于 version 1 的 migration（包括 V1）都会被跳过。因此 legacy baseline 不会重新建表，也不会比较或修复实际 schema。

## 默认配置

```env
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false
```

默认和日常运行必须保持 baseline-on-migrate 为 `false`。只有已存在业务表、没有 `flyway_schema_history`、且已经确认 schema 与 V1 等价的 legacy 数据库，才允许在首次接入时临时设置为 `true`。

项目同时保持以下安全设置：

- migration 启动时校验；
- 不允许 out-of-order migration；
- 禁用 Flyway clean；
- 已应用 migration 不允许修改。

`validate-on-migrate` 校验 migration history、文件和 checksum，不会把真实表结构与 V1 做完整 diff。

## V1 范围

V1 创建以下 13 张当前业务表：

- `pm_user`
- `pm_user_plan`
- `pm_credit_log`
- `pm_project`
- `pm_project_file`
- `pm_project_qa_record`
- `pm_feedback`
- `pm_analysis_report`
- `pm_report_share`
- `pm_interview_session`
- `pm_interview_message`
- `pm_analysis_task`
- `pm_ai_call_log`

V1 只固化接入 Flyway 前的既有设计，不顺便治理历史问题。它继续保留：

- `pm_user.email` 没有数据库 UNIQUE；
- `pm_user_plan.remaining_credits` 的历史默认值为 `3`；
- `pm_project_file` 没有 `(project_id, file_path)` 组合唯一索引；
- 业务表之间没有外键。

这些内容只能通过 V2 及之后的新 migration 处理。

## 全新数据库流程

适用于完全空的新环境：

1. 创建空 database；默认名称为 `projectmentor_ai`，字符集使用 `utf8mb4`，排序规则使用 `utf8mb4_unicode_ci`。
2. 设置 `FLYWAY_ENABLED=true`、`FLYWAY_BASELINE_ON_MIGRATE=false`。
3. 启动 MySQL，再启动 backend。
4. backend 启动期间 Flyway 创建 `flyway_schema_history` 并执行 V1。
5. 验证 V1 状态成功、13 张业务表存在、`/api/health` 正常。

Docker Compose 下 MySQL 通过 `MYSQL_DATABASE` 创建空 database，Flyway 由 backend 执行。不要再运行或挂载旧 `init.sql`。

## Legacy 数据库首次接入

适用于已有业务表、已有真实数据、但尚无 `flyway_schema_history` 的数据库。

### 1. 备份与确认

```bash
cd /opt/projectmentor-ai
bash scripts/backup-mysql.sh
```

确认备份成功并另行妥善保存，同时确认旧 backend 和关键业务功能正常。不要在没有可用备份时开始 baseline。

### 2. 核对 schema

将实际数据库与 V1 逐项核对，至少包括：

- 13 张业务表是否齐全；
- 列名、类型、长度和 nullability；
- 默认值和 `ON UPDATE`；
- 主键、唯一约束与普通索引；
- engine、字符集和排序规则；
- 是否已经存在 `flyway_schema_history`。

历史数据库可能缺少 `pm_project_qa_record`、`pm_feedback` 或 `pm_analysis_report.claim_evidence`，也可能仍保留旧 `tech_stack` 类型。发现任何不一致时停止 baseline；不要假定 Flyway 会补齐，也不要直接复制旧文档中的手工 ALTER。

### 3. 仅首次开启 baseline

在服务器 `.env` 中临时设置：

```env
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=true
```

使用已经完成本地和 CI 验证的新 backend 启动。Flyway 应只新增 history / baseline metadata，不执行 V1，不修改现有业务表。

### 4. 验证并关闭

检查 `flyway_schema_history`，确认存在 version 1 baseline 记录；再检查 backend 健康状态和登录、项目、报告、问答、反馈、额度等关键功能。

验证完成后立即恢复：

```env
FLYWAY_BASELINE_ON_MIGRATE=false
```

重新创建 backend 容器以确保环境变量生效，并再次检查日志与健康状态。baseline-on-migrate 不应长期保持开启，因为它会降低误连未知非空数据库时的保护。

如果数据库已经存在 `flyway_schema_history`，不要再次 baseline。

## 后续 migration 规则

文件名格式：

```text
V{version}__{snake_case_description}.sql
```

例如：

```text
V2__schema_integrity_constraints.sql
```

规则：

1. 已应用 migration 永不修改、重命名或删除。
2. 每个 migration 只承担一个清晰的 schema 目的。
3. version 必须为正整数，不允许重复。
4. 生产变更前先备份，再部署包含新 migration 的 backend。
5. migration 失败时停止部署并分析原因，不关闭 Flyway、不修改 history 表、不运行 clean。
6. 已上线 migration 如需修正，新增下一个 version；不要回写旧文件。
7. 不用生产库手工 ALTER 替代 Git 中的 migration。

提交前运行：

```bash
bash scripts/check-db-migrations.sh
```

该脚本检查 migration 目录和 V1 是否存在、文件是否为已纳入 Git 的非空常规文件、命名是否合法，以及 version 是否为正整数且不重复。它只输出路径、version 和 PASS / FAIL，不读取或输出 SQL 内容，也不验证 SQL 业务语义。新增 migration 应先暂存，再在提交前运行该脚本。

## 备份与恢复

`scripts/backup-mysql.sh` 使用 `mysqldump` 备份整个业务 database，没有排除 `flyway_schema_history`。baseline 后创建的备份会自然包含该表，`scripts/restore-mysql.sh` 也会将其一并恢复。

### 恢复带 history 的备份

1. 保持 backend 停止。
2. 恢复 SQL 备份。
3. 保持 `FLYWAY_BASELINE_ON_MIGRATE=false`。
4. 启动 backend；Flyway 校验现有 history，并从记录版本之后继续执行 pending migrations。
5. 验证 backend 和关键业务数据。

### 恢复无 history 的旧备份

1. 保持 backend 停止并恢复备份。
2. 核对恢复后的实际 schema 与 V1 是否等价。
3. 等价时按 legacy 首次接入流程建立 baseline。
4. 不等价时停止，先制定显式修复方案。

仅仅成功恢复 SQL 文件不代表 schema 已达到 V1，也不代表后续 migration 一定安全。

## CI 与本地验证

CI 的 database migration smoke test 使用两个临时 MySQL service 和假凭据验证：

- fresh 空 database 能执行 V1，13 张业务表存在，history 中只有成功的 V1 SQL 记录，backend 能健康启动；
- legacy 非空 schema 在 `baseline-on-migrate=true` 时建立成功的 version 1 `BASELINE` history，不执行 V1 SQL。

CI 不连接生产数据库、不使用生产密码、不执行生产 baseline，也不自动部署。生产 legacy baseline 必须由维护者在备份和 schema 核对后单独执行。

## 明确不提供的能力

- 自动数据库回滚；
- 零停机 migration 保证；
- 自动修复 legacy schema drift；
- 完整灾难恢复或恢复演练；
- 企业级数据库变更审批平台。

任何生产 migration 都应结合备份、变更审查、维护窗口和业务验证执行。

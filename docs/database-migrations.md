# 数据库迁移与 Schema 版本治理

V4.9-0 起，ProjectMentor AI 使用 Flyway 管理业务 schema；V4.9-1 通过 V2 治理关键唯一约束、默认值和项目删除索引。本文说明全新数据库、两种生产升级状态、已有 legacy 数据库、备份恢复和后续 migration 的安全流程。

这套机制用于建立可重复的 schema 版本历史，不代表自动回滚、零停机迁移、完整 schema drift detection、企业级 DBA 平台或灾难恢复系统。

## 唯一真源与术语

业务 schema 的唯一真源是：

```text
backend/projectmentor-server/src/main/resources/db/migration/V*.sql
```

当前 migration 是：

```text
V1__baseline_schema.sql
V2__schema_integrity_constraints.sql
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

## V1 与 V2 范围

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

V1 只固化接入 Flyway 前的既有设计，不顺便治理历史问题。V1 中的历史状态包括：

- `pm_user.email` 没有数据库 UNIQUE；
- `pm_user_plan.remaining_credits` 的历史默认值为 `3`；
- `pm_project_file` 没有 `(project_id, file_path)` 组合唯一索引；
- 业务表之间没有外键。

V1 已经不可变，不允许为修正这些设计而回写。V4.9-1 新增的 `V2__schema_integrity_constraints.sql` 负责：

- 为 `pm_user.email` 增加唯一索引 `uk_user_email`；
- 为 `pm_project_file(project_id, file_path)` 增加组合唯一索引 `uk_project_file_path`；
- 只将 `pm_user_plan.remaining_credits` 的列默认值从 `3` 改为 `10`，不修改任何既有用户余额；
- 为 `pm_project_qa_record.project_id` 增加 `idx_qa_project_id`，支持项目删除按 project 精确清理 QA。

V2 不增加业务表，不自动删除、合并或改写历史重复数据，也不增加全库 foreign keys / `ON DELETE CASCADE`。如果 UNIQUE 变更遇到历史重复，migration 应失败并停止部署。

## V4.9-1 / V2 生产部署 preflight

执行 V2 前必须先生成并妥善保存数据库备份，然后在目标数据库上执行以下两条只读查询。

重复邮箱：

```sql
SELECT email, COUNT(*) AS cnt
FROM pm_user
GROUP BY email
HAVING COUNT(*) > 1;
```

同一项目内的重复文件路径：

```sql
SELECT project_id, file_path, COUNT(*) AS cnt
FROM pm_project_file
GROUP BY project_id, file_path
HAVING COUNT(*) > 1;
```

两条查询都必须返回 **0 rows** 才能继续。如果任一查询返回记录：**STOP**，不要部署 V2；由维护者人工核对业务归属、数据内容与修复影响，再制定经过审查的显式修复方案。不要自动 `DELETE`、不要保留 `MIN(id)` / `MAX(id)`、不要自动合并用户，也不要由 migration 改写业务数据。preflight 只输出 email、project_id、file_path 与 count，不查询或打印 password、文件 content、token 或 secret。

### 生产状态 A：已经部署 V4.9-0

适用于 `flyway_schema_history` 已存在成功的 version 1（通常为 legacy 数据库的 `BASELINE`，fresh 数据库则为 `SQL`）记录：

1. 备份数据库并确认备份可用。
2. 执行上述两条 duplicate preflight，必须均为 0 rows。
3. 保持 `FLYWAY_ENABLED=true`、`FLYWAY_BASELINE_ON_MIGRATE=false`。
4. 部署并启动 V4.9-1 backend；Flyway 应执行 `V2__schema_integrity_constraints.sql`。
5. 确认 history 中新增成功的 `V2 SQL`，13 张业务表仍存在，既有余额和业务数据保持不变，再验证 backend 健康与关键业务。

### 生产状态 B：仍是无 history 的 legacy 数据库

适用于业务表和真实数据已经存在，但尚无 `flyway_schema_history`：

1. 备份数据库，逐项确认实际 schema 与不可变 V1 等价。
2. 执行上述两条 duplicate preflight，必须均为 0 rows。
3. 仅首次临时设置 `FLYWAY_BASELINE_ON_MIGRATE=true`，启动已经验证过的 V4.9-1 backend。
4. 预期 Flyway history 为成功的 `V1 BASELINE` 与 `V2 SQL`；V1 不执行，V2 正常执行。
5. 验证原有用户、项目、项目文件、余额和其他关键数据均保留，检查 V2 索引/default 与 backend 健康。
6. 立即恢复 `FLYWAY_BASELINE_ON_MIGRATE=false`，强制重新创建 backend 容器并再次检查日志、history 与健康状态。

不要让生产长期保持 baseline-on-migrate 为 `true`。如果 history 已存在，不要再次 baseline。

## 全新数据库流程

适用于完全空的新环境：

1. 创建空 database；默认名称为 `projectmentor_ai`，字符集使用 `utf8mb4`，排序规则使用 `utf8mb4_unicode_ci`。
2. 设置 `FLYWAY_ENABLED=true`、`FLYWAY_BASELINE_ON_MIGRATE=false`。
3. 启动 MySQL，再启动 backend。
4. backend 启动期间 Flyway 创建 `flyway_schema_history`，按顺序执行 V1、V2。
5. 验证 history 为成功的 `V1 SQL`、`V2 SQL`，13 张业务表和 V2 约束/default/索引存在，`/api/health` 正常。

Docker Compose 下 MySQL 通过 `MYSQL_DATABASE` 创建空 database，Flyway 由 backend 执行。不要再运行或挂载旧 `init.sql`。

## Legacy 数据库首次接入详解

适用于已有业务表、已有真实数据、但尚无 `flyway_schema_history` 的数据库。

### 1. 备份与确认

```bash
cd /opt/projectmentor-ai
bash scripts/backup-mysql.sh
```

确认备份成功并另行妥善保存，同时确认旧 backend 和关键业务功能正常。不要在没有可用备份时开始 baseline；继续前还必须完成上一节的 V2 duplicate preflight。

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

使用已经完成本地和 CI 验证的 V4.9-1 backend 启动。Flyway 应建立 version 1 baseline、不执行 V1，然后继续执行 V2。V2 会新增约束/索引并修改列 default，但不会批量修改余额或删除业务数据。

### 4. 验证并关闭

检查 `flyway_schema_history`，确认存在成功的 `V1 BASELINE` 与 `V2 SQL`；再检查 13 张业务表、V2 约束/default/索引、原有哨兵数据、backend 健康状态和登录、项目、报告、问答、反馈、额度等关键功能。

验证完成后立即恢复：

```env
FLYWAY_BASELINE_ON_MIGRATE=false
```

重新创建 backend 容器以确保环境变量生效，并再次检查日志与健康状态。baseline-on-migrate 不应长期保持开启，因为它会降低误连未知非空数据库时的保护。

如果数据库已经存在 `flyway_schema_history`，不要再次 baseline；按“生产状态 A”保持 `false` 并执行 pending V2。

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
3. 执行 V2 duplicate preflight，两条查询必须均为 0 rows。
4. 等价且 preflight 通过时，按 legacy 首次接入流程建立 V1 baseline 并执行 V2。
5. 不等价或发现重复时停止，先制定显式修复方案。

仅仅成功恢复 SQL 文件不代表 schema 已达到 V1，也不代表后续 migration 一定安全。

## CI 与本地验证

CI 的 database migration smoke test 使用两个临时 MySQL service 和假凭据验证：

- fresh 空 database 能执行 V1、V2，13 张业务表存在，history 为成功的 `V1 SQL`、`V2 SQL`，V2 schema 断言和 backend 健康检查通过；
- legacy 非空 schema 预先保留 user、plan、project、project_file 哨兵数据，在 `baseline-on-migrate=true` 时建立 `V1 BASELINE`、跳过 V1 SQL 并执行 `V2 SQL`，迁移后哨兵数据和既有余额保持不变；
- CI 断言 email UNIQUE、project file 组合 UNIQUE 的列顺序、credits default 10 与 QA project 索引，并通过注册、创建两个项目和 README API 真实调用 Java Mapper，验证同项目同路径更新为一条、不同项目相同路径分别存在。

CI 不连接生产数据库、不使用生产密码、不执行生产 baseline，也不自动部署。生产 legacy baseline 必须由维护者在备份和 schema 核对后单独执行。

## 明确不提供的能力

- 自动数据库回滚；
- 零停机 migration 保证；
- 自动修复 legacy schema drift；
- 完整灾难恢复或恢复演练；
- 企业级数据库变更审批平台。

任何生产 migration 都应结合备份、变更审查、维护窗口和业务验证执行。

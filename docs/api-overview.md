# ProjectMentor AI API 概览

说明：

- 除注册、登录和健康检查外，`/api/**` 默认需要登录。
- 登录后前端通过 `Authorization: Bearer <token>` 访问接口。
- 返回结构使用统一 `Result` 包装。

## Auth

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/auth/email-code` | 发送注册邮箱验证码；未登录可调用，受邮箱和 IP 频率限制 | 否 |
| `POST` | `/api/auth/register` | 注册用户并返回 token；开启邮箱验证码后需要提交验证码；成功注册受同 IP 每小时 3 次、每天 10 次限制 | 否 |
| `POST` | `/api/auth/login` | 用户登录并返回 token | 否 |
| `GET` | `/api/auth/me` | 获取当前登录用户信息 | 是 |
| `POST` | `/api/auth/logout` | 退出登录；当前版本主要由前端清理 token | 是 |

## Project

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/projects` | 创建项目 | 是 |
| `GET` | `/api/projects` | 获取当前用户项目列表 | 是 |
| `GET` | `/api/projects/{id}` | 获取项目详情 | 是 |
| `DELETE` | `/api/projects/{id}` | 删除项目 | 是 |

## File

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/projects/{projectId}/readme` | 保存或更新 README 内容 | 是 |
| `GET` | `/api/projects/{projectId}/files` | 获取项目文件列表 | 是 |
| `GET` | `/api/projects/{projectId}/files/{fileId}` | 获取项目文件详情 | 是 |
| `DELETE` | `/api/projects/{projectId}/files/{fileId}` | 删除项目文件 | 是 |
| `POST` | `/api/projects/{projectId}/upload-zip` | 上传并解析 ZIP 文件 | 是 |

## Scanner

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/projects/{projectId}/scan` | 执行规则扫描并返回风险点与证据链 | 是 |

## Analysis Report

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/projects/{projectId}/reports/generate` | 同步生成 AI 审计报告，消耗 2 credits；AI 失败退款并保存规则版报告 | 是 |
| `GET` | `/api/projects/{projectId}/reports` | 获取项目报告列表 | 是 |
| `GET` | `/api/reports/{reportId}` | 获取报告详情 | 是 |
| `POST` | `/api/reports/{reportId}/claim-evidence/ai-enhance` | AI 深度解读已生成的 Claim-Evidence Matrix，消耗 2 credits，失败返还 | 是 |

## Analysis Task

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/projects/{projectId}/analyze` | 启动异步 AI 审计任务，任务成本为 2 credits | 是 |
| `GET` | `/api/tasks/{taskId}` | 查询异步任务进度 | 是 |

## Project Q&A

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/projects/{projectId}/qa` | 有证据时调用 AI 回答，消耗 1 credit；AI 失败退款并返回规则检索结果 | 是 |
| `GET` | `/api/projects/{projectId}/qa/history` | 查看最近问答历史，不扣额度 | 是 |
| `DELETE` | `/api/projects/{projectId}/qa/history/{recordId}` | 删除自己的问答历史，不扣额度 | 是 |

## Hallucination

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/hallucination/check` | AI 幻觉检测，消耗 1 credit；AI 失败退款并返回规则结果 | 是 |

## Interview

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/interview/start` | 启动 AI 模拟面试，消耗 2 credits；AI 首题失败退款并转规则会话 | 是 |
| `POST` | `/api/interview/{sessionId}/answer` | 提交回答并获得本场面试内的追问与反馈，不重复扣费 | 是 |
| `GET` | `/api/interview/{sessionId}` | 查看面试会话详情，不扣额度 | 是 |
| `POST` | `/api/interview/{sessionId}/finish` | 结束面试并生成规则复盘，不重复扣费 | 是 |

## Credit

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `GET` | `/api/credits/me` | 查看当前用户额度 | 是 |
| `GET` | `/api/credits/logs` | 查看当前用户额度流水 | 是 |
| `GET` | `/api/admin/credits/users` | 分页查询用户余额、累计消耗、累计返还和管理员发放 | 是，且需要管理员权限 |
| `GET` | `/api/admin/credits/users/{userId}/logs` | 分页查询指定用户额度流水，支持类型、模块和时间筛选 | 是，且需要管理员权限 |
| `POST` | `/api/admin/credits/users/{userId}/grant` | 管理员发放额度并写入 `ADMIN_GRANT` 流水 | 是，且需要管理员权限 |
| `POST` | `/api/admin/credits/users/{userId}/deduct` | 管理员扣除额度并写入 `ADMIN_DEDUCT` 流水，余额不能为负数 | 是，且需要管理员权限 |
| `POST` | `/api/admin/credits/add` | 旧版管理员增加额度兼容接口 | 是，且需要管理员权限 |

额度不足统一返回业务错误码 `60001`，并且不会调用 LLM。所有退款使用独立额度事务写入对应 `*_REFUND` 流水。

## Admin AI Usage

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/ai-usage/overview` | 基于额度流水统计今日 / 累计 AI 调用、消耗、退款、模块排行、用户排行和最近 AI 流水 | 是，且需要管理员权限 |

管理员接口由后端 `AdminInterceptor` 强制校验。前端隐藏入口只用于体验优化，普通用户直接请求 `/api/admin/**` 仍会返回无权限。

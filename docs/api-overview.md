# ProjectMentor AI API 概览

说明：

- 除注册、登录和健康检查外，`/api/**` 默认需要登录。
- 登录后前端通过 `Authorization: Bearer <token>` 访问接口。
- 返回结构使用统一 `Result` 包装。

## Auth

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | 注册用户并返回 token | 否 |
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
| `POST` | `/api/projects/{projectId}/reports/generate` | 同步生成项目审计报告 | 是 |
| `GET` | `/api/projects/{projectId}/reports` | 获取项目报告列表 | 是 |
| `GET` | `/api/reports/{reportId}` | 获取报告详情 | 是 |

## Analysis Task

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/projects/{projectId}/analyze` | 启动异步分析任务 | 是 |
| `GET` | `/api/tasks/{taskId}` | 查询异步任务进度 | 是 |

## Hallucination

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/hallucination/check` | 检测 AI 回答中的幻觉、过度鼓励和简历风险 | 是 |

## Interview

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `POST` | `/api/interview/start` | 启动模拟面试会话 | 是 |
| `POST` | `/api/interview/{sessionId}/answer` | 提交回答并获得追问与反馈 | 是 |
| `GET` | `/api/interview/{sessionId}` | 查看面试会话详情 | 是 |
| `POST` | `/api/interview/{sessionId}/finish` | 结束面试并生成总结 | 是 |

## Credit

| 方法 | 路径 | 用途 | 是否需要登录 |
| --- | --- | --- | --- |
| `GET` | `/api/credits/me` | 查看当前用户额度 | 是 |
| `GET` | `/api/credits/logs` | 查看当前用户额度流水 | 是 |
| `POST` | `/api/admin/credits/add` | 管理员给用户增加额度 | 是，且需要管理员角色 |

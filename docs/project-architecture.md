# ProjectMentor AI 项目架构说明

## 系统分层

```text
浏览器
  ↓
Vue 3 前端页面
  ↓ Axios
Nginx / 本地 Vite 代理
  ↓
Spring Boot Controller
  ↓
Service 业务层
  ↓
MyBatis-Plus Mapper
  ↓
MySQL / Redis

AI 增强模块通过 OpenAI-compatible API 调用外部模型；
当 AI Key 未配置或调用失败时，报告降级为规则版结果。
```

## 后端模块说明

| 模块 | 说明 |
| --- | --- |
| `auth` | 用户注册、登录、JWT 生成、当前用户信息、注册 IP 频率限制 |
| `project` | 项目创建、列表、详情和删除 |
| `file` | README 保存、项目文件查询、ZIP 上传解析 |
| `scanner` | README 风险规则扫描与证据链生成 |
| `analysis` | 审计报告生成、异步分析任务、Redis 进度 |
| `hallucination` | AI 回答幻觉和简历风险检测 |
| `interview` | 规则版模拟面试会话、追问、评分和总结 |
| `credit` | 额度账户、额度流水、管理员发放 / 扣除、AI 使用统计数据源 |
| `ai` | OpenAI-compatible API 客户端与 AI 审计结果结构 |
| `common` | 统一返回、错误码、业务异常和全局异常处理 |
| `config` | CORS、MVC 拦截器、异步线程池、Knife4j 等配置 |

## 前端模块说明

| 模块 | 说明 |
| --- | --- |
| `views` | 登录注册、仪表盘、项目、报告、幻觉检测、面试、额度页面 |
| `api` | Axios 请求封装和各业务模块 API |
| `components` | 风险列表、证据链、任务进度、评分环、图表、Markdown 渲染等组件 |
| `stores` | Pinia 用户状态与登录信息 |
| `router` | Vue Router 页面路由与登录态拦截 |
| `layouts` | 主布局、侧边栏和顶部栏 |

## 数据库核心表

| 表名 | 说明 |
| --- | --- |
| `pm_user` | 用户账号、密码摘要、邮箱、角色和状态 |
| `pm_user_plan` | 用户套餐类型和剩余额度 |
| `pm_credit_log` | 额度增加、消耗和返还流水 |
| `pm_project` | 项目基础信息、技术栈和分析状态 |
| `pm_project_file` | README、配置、代码、SQL 等解析后的项目文件 |
| `pm_analysis_report` | 审计报告评分、风险点、证据链、建议和简历写法 |
| `pm_analysis_task` | 异步分析任务状态、进度、报告 ID 和失败原因 |
| `pm_interview_session` | 模拟面试会话状态、总分和总结 |
| `pm_interview_message` | 面试问答消息、单次评分和反馈 |

## 规则扫描与证据链流程

```text
用户上传 README / ZIP
  ↓
项目文件写入 pm_project_file
  ↓
ProjectRuleScanner 读取当前用户项目文件
  ↓
ReadmeRiskScanner 检查 README 表述
  ↓
根据文件路径、文件类型、配置和代码关键词形成证据
  ↓
输出 RiskItem 与 EvidenceItem
  ↓
AnalysisReportService 生成报告并落库
```

当前规则重点关注：

- README 是否缺失。
- README 中的技术描述是否有对应文件或配置证据。
- Docker、Redis、JWT、RAG 等能力是否能在上传文件中找到支撑。
- 项目文件数量、配置文件、SQL、Maven、Dockerfile 等工程化证据。

## 异步分析任务流程

```text
POST /api/projects/{projectId}/analyze
  ↓
创建 pm_analysis_task，状态 PENDING
  ↓
Redis 写入初始进度
  ↓
@Async 后台执行分析
  ↓
规则扫描 + AI 增强尝试 + 报告落库
  ↓
任务状态更新为 SUCCESS / FAILED
  ↓
前端轮询 GET /api/tasks/{taskId}
```

说明：

- Redis 用于缓存任务进度，过期时间为 2 小时。
- MySQL 中仍保留任务状态，Redis 读取失败时可以回退到数据库。
- 报告生成失败时会记录失败原因；如果已扣除额度，会执行额度返还。

## 额度系统流程

```text
注册用户
  ↓
创建 FREE 套餐，赠送 10 credits，并记录 REGISTER_GIFT 流水
  ↓
进入 AI 功能前按模块预扣额度
  ↓
写入 pm_credit_log
  ↓
调用 OpenAI-compatible LLM
  ↓
成功：保留扣费流水
  ↓
失败 / 返回不可用 / 业务结果保存失败：返还额度并记录对应 *_REFUND 流水
```

当前统一成本为：AI 审计报告 2、AI Claim-Evidence 2、AI 项目问答 1、AI 幻觉检测 1、AI 模拟面试 2、独立 AI 简历优化 1 credit。规则扫描、上传、项目管理、Dashboard、历史查看和公开分享不扣额度。

当前额度系统覆盖额度账户、AI 消耗、失败返还和管理员发放 / 扣除，尚未接入支付系统。AI 使用统计直接聚合 `pm_credit_log`，继续复用 `pm_user_plan` 与 `pm_credit_log`，不新增数据库字段或用量表。

## 注册防刷流程

```text
POST /api/auth/register
  ↓
从 X-Forwarded-For / X-Real-IP / remoteAddr 获取客户端 IP
  ↓
检查 register:ip:hour:* 与 register:ip:day:* 计数
  ↓
每小时最多成功注册 3 个账号，每天最多 10 个账号
  ↓
注册成功后递增 Redis 与进程内影子计数
```

Redis 用于跨实例限流，进程内计数用于安全降级和单实例兜底。Redis 读取或写入失败时只记录告警，不会让注册接口崩溃；登录流程不受该限制影响。

## Docker 部署架构

```text
Nginx :80
├── 静态资源：frontend_dist
└── /api 反向代理到 backend:8080

backend
├── 连接 mysql:3306
├── 连接 redis:6379
└── 可选调用 AI_BASE_URL

mysql
└── 使用 init.sql 初始化数据库结构

redis
└── 存储异步分析任务进度缓存
```

Compose 服务：

- `mysql`：MySQL 8，挂载初始化 SQL。
- `redis`：Redis 7 alpine。
- `backend`：Spring Boot 后端服务。
- `frontend`：构建前端静态文件并写入共享卷。
- `nginx`：提供前端访问入口和 `/api` 反向代理。

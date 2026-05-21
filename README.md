# ProjectMentor AI｜AI 项目真实性审计与面试深挖平台

面向计算机学生和后端实习求职者的项目真实性审计工具，帮助用户识别 README 夸大、AI 回答幻觉、简历风险和面试追问风险。

## 项目背景

很多学生开始使用 AI 辅助做项目、写 README、整理简历和准备面试，但 AI 生成内容容易带来新的风险：

- README 描述比真实代码实现更“满”。
- 简历描述找不到对应代码、配置或运行证据。
- AI 评价过度鼓励，让用户高估项目成熟度。
- 面试时被追问到具体实现，容易解释不清。
- 不知道项目是否适合写进简历，也不知道该如何保守表达。

ProjectMentor AI 的定位不是“让 AI 直接打分”，而是先做规则扫描和证据链整理，再用 AI 做表达增强和审计补充。AI 不可用时，系统仍然可以输出规则版报告。

## 核心功能

| 功能 | 状态 | 说明 |
| --- | --- | --- |
| 用户认证 | 已完成 | 支持注册、登录、JWT 鉴权、BCrypt 密码加密 |
| 项目管理 | 已完成 | 支持创建、列表、详情、删除项目 |
| README 保存 | 已完成 | 支持粘贴 README 并保存为项目文件 |
| ZIP 上传解析 | 已完成 | 支持上传 ZIP，解析白名单文本文件并过滤风险路径 |
| 规则扫描 | 已完成 | 基于 README 与项目文件做风险识别 |
| 证据链 | 已完成 | 对关键结论关联文件、配置或代码证据 |
| 审计报告 | 已完成 | 输出评分、风险点、建议和简历改写版本 |
| AI 增强报告 | 基础版 | 支持 OpenAI-compatible API；AI 不可用时降级为规则报告 |
| AI 幻觉检测 | 已完成 | 检测 AI 回答中的过度鼓励、缺少证据和简历风险 |
| 面试深挖 | 基础版 | 规则版 V1，支持会话、追问、评分和总结 |
| 额度系统 | 已完成 | 注册赠送额度，报告生成消耗额度，失败返还并记录流水 |
| 异步任务 | 已完成 | 支持异步分析任务，Redis 缓存任务进度 |
| 前端页面 | 已完成 | Vue 3 页面覆盖主要业务流程 |
| Docker 部署 | 已完成 | 支持 Docker Compose 启动 MySQL、Redis、后端、前端和 Nginx |

## 产品流程图

```mermaid
flowchart LR
    A[注册登录] --> B[创建项目]
    B --> C[上传 README / ZIP]
    C --> D[规则扫描]
    D --> E[生成报告]
    E --> F[查看证据链]
    F --> G[简历优化]
    G --> H[模拟面试]
```

## 技术架构

后端：

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL 8
- Redis
- JWT
- BCrypt
- Validation
- AOP
- `@Async`
- Docker

前端：

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts
- markdown-it

部署：

- Docker Compose
- Nginx
- MySQL
- Redis

AI：

- OpenAI-compatible API
- 支持 DeepSeek / 豆包 / OpenAI 风格接口
- AI Key 未配置或调用失败时，自动降级为规则报告

## 核心设计亮点

- 不直接相信 README：README 只是待验证材料，系统会结合上传文件做规则扫描。
- 不直接相信 AI 回答：AI 幻觉检测会识别过度鼓励、缺少证据和不适合写入简历的表述。
- 先规则扫描，再 AI 增强：规则扫描负责稳定输出，AI 负责补充表达与总结。
- 每个关键结论尽量带证据：通过文件类型、路径、配置和代码片段形成证据链。
- 用户数据按 `userId` 隔离：项目、任务、报告、额度和面试会话都按当前用户校验。
- ZIP 上传安全过滤：用户可以直接上传项目 ZIP，系统会自动过滤 `.git`、`target`、`node_modules`、`dist`、`build` 等目录，当前最大支持 50MB，并且只解析核心文本文件，不保存二进制文件。
- 异步任务避免接口阻塞：分析任务进入后台执行，前端轮询任务状态。
- 额度流水形成商业化雏形：当前只实现额度账户、消耗、返还和管理员加额，尚未接入支付系统。

## 项目结构

```text
projectmentor-ai
├── backend
│   └── projectmentor-server
├── frontend
│   └── projectmentor-web
├── deploy
│   └── nginx
└── docs
```

## 本地开发运行方式

后端：

1. 创建 MySQL 数据库 `projectmentor_ai`。
2. 执行初始化脚本：`backend/projectmentor-server/src/main/resources/db/init.sql`。
3. 配置环境变量，例如 `DB_HOST`、`DB_PORT`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`。
4. 启动后端：

```bash
cd backend/projectmentor-server
mvn spring-boot:run
```

前端：

```bash
cd frontend/projectmentor-web
npm install
npm run dev
```

默认本地访问：

- 后端健康检查：`http://localhost:8080/api/health`
- 前端开发服务：以 Vite 输出为准，通常为 `http://localhost:5173`

## Docker Compose 运行方式

```bash
cp .env.example .env
```

修改 `.env` 中的关键配置：

- `MYSQL_ROOT_PASSWORD`
- `JWT_SECRET`
- 可选：`AI_BASE_URL`、`AI_API_KEY`、`AI_MODEL`

启动：

```bash
docker compose up -d --build
```

访问：

```text
http://localhost
```

## 环境变量说明

| 变量 | 说明 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码，Docker Compose 启动时必填 |
| `JWT_SECRET` | JWT 签名密钥，请使用足够长的随机字符串 |
| `AI_BASE_URL` | OpenAI-compatible API 地址，例如 DeepSeek 或其他兼容接口 |
| `AI_API_KEY` | AI 服务密钥，可留空；留空时使用规则版报告 |
| `AI_MODEL` | AI 模型名称，例如 `deepseek-chat` |

不要提交真实 `.env` 文件或真实密钥。仓库中只保留 `.env.example`。

## API 模块概览

- `auth`：注册、登录、当前用户、退出登录。
- `projects`：项目创建、列表、详情、删除。
- `files`：README 保存、文件列表、文件详情、删除文件。
- `upload-zip`：上传项目 ZIP，自动过滤无关目录和二进制文件，并解析核心文本文件。
- `scan`：README 风险规则扫描与证据链生成。
- `reports`：生成审计报告、查看项目报告列表、查看报告详情。
- `analyze tasks`：启动异步分析任务、查询任务进度。
- `hallucination`：检测 AI 回答幻觉和简历风险。
- `interview`：启动模拟面试、提交回答、查看会话、结束面试。
- `credits`：查看额度、额度流水、管理员加额。

更完整的接口说明见 [docs/api-overview.md](docs/api-overview.md)。

## 演示流程

1. 注册新用户，系统赠送初始分析额度。
2. 登录后进入项目列表。
3. 创建一个项目，填写名称、技术栈和项目描述。
4. 上传普通项目 ZIP，系统会自动过滤 `.git`、`target`、`node_modules`、`dist`、`build` 等目录，只解析 README、配置文件、Java 文件、SQL 文件等核心文本文件。
5. 执行规则扫描，查看 README 风险点和证据链。
6. 启动异步分析任务，等待报告生成。
7. 查看项目审计报告，包括评分、风险点、证据链、建议和简历写法。
8. 粘贴一段 AI 对项目的评价，执行 AI 幻觉检测。
9. 启动模拟面试，回答追问并查看反馈。

## 当前限制

- AI Key 未配置时使用规则版报告，不会调用外部 AI 服务。
- 面试深挖当前是规则版 V1，追问逻辑还需要继续扩展。
- 尚未接入真实支付，当前只有额度账户和流水。
- ZIP 上传当前最大支持 50MB，会自动过滤常见依赖、构建和 IDE 目录，只解析核心文本文件，不保存二进制文件。
- 还需要真实用户测试，用于调整规则、文案和演示流程。

## 后续路线

- LLM 深度增强：优化提示词、结构化输出和错误兜底。
- RAG 项目问答：基于项目文件做检索增强问答。
- PDF 导出：支持导出审计报告和面试复盘。
- 管理员后台：补充用户、额度、报告和任务管理页面。
- 线上部署：整理服务器部署、域名、HTTPS 和运维说明。
- 用户反馈收集：记录用户对风险识别、报告建议和面试追问的反馈。

## 简历写法

可使用的真实可信版本：

```text
ProjectMentor AI：面向计算机学生的项目真实性审计与面试准备工具。项目基于 Spring Boot 3 + Vue 3 实现，支持用户登录、项目管理、README 保存、ZIP 文本文件解析、规则扫描、证据链展示、审计报告、AI 幻觉检测、规则版模拟面试、额度流水和 Docker Compose 本地部署。系统采用“规则扫描 + 证据链 + AI 增强”的设计，AI 服务不可用时可降级为规则版报告，适合作为全栈 MVP 和后端实习项目展示。
```

面试时可以重点讲：

- 为什么要先做规则扫描，而不是直接把 README 丢给 AI。
- 如何用 `userId` 做项目、报告、任务和额度隔离。
- ZIP 上传如何过滤危险路径、目录和文件类型。
- 异步任务如何用 `@Async` 执行，并用 Redis 缓存进度。
- 额度系统如何记录消耗、返还和管理员加额流水。

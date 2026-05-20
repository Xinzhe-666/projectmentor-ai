# ProjectMentor AI 面试讲解准备

## 项目一句话介绍

ProjectMentor AI 是一个面向计算机学生的项目真实性审计与面试准备工具，通过规则扫描、证据链和 AI 增强，帮助用户识别 README 夸大、AI 回答幻觉、简历风险和面试追问风险。

## 为什么做这个项目

现在很多学生会用 AI 辅助做项目和写简历，但 AI 经常会把项目评价得过于乐观。问题是：README 写得很漂亮，不代表代码里真的有对应实现；AI 说可以写进简历，也不代表面试能解释清楚。

所以这个项目选择从“项目真实性”切入，不是简单做一个聊天工具，而是先检查 README、代码文件、配置和证据，再给出更保守的简历和面试建议。

## 核心业务流程

```text
注册登录
  ↓
创建项目
  ↓
粘贴 README 或上传 ZIP
  ↓
规则扫描
  ↓
生成审计报告
  ↓
查看风险点和证据链
  ↓
AI 幻觉检测 / 模拟面试
```

## 技术架构

后端使用 Spring Boot 3 + MyBatis-Plus + MySQL + Redis。前端使用 Vue 3 + Vite + TypeScript + Element Plus。部署侧使用 Docker Compose 编排 MySQL、Redis、后端、前端静态资源和 Nginx。

AI 模块使用 OpenAI-compatible API，支持 DeepSeek、豆包或 OpenAI 风格接口。AI 不可用时，系统降级为规则版报告。

## JWT 鉴权怎么做

注册或登录成功后，后端生成 JWT，返回给前端。前端把 token 存在本地，并在后续请求中通过 `Authorization: Bearer <token>` 传给后端。

后端使用 `AuthInterceptor` 拦截 `/api/**` 请求，排除健康检查、注册和登录接口。拦截器会验证 token，验证通过后从 token 中解析 `userId`，写入 `UserContext`，业务层通过 `UserContext.getUserId()` 获取当前用户。

## userId 数据隔离怎么做

项目、任务、报告、面试和额度接口都会读取当前登录用户的 `userId`。查询项目时会带上 `project_id` 和 `user_id` 条件，避免用户访问不属于自己的项目。

例如生成报告前会先检查项目是否属于当前用户；查看报告时，也会通过报告关联的项目反查 `userId`。

## 规则扫描怎么做

规则扫描分为两层：

- `ProjectRuleScanner`：读取项目文件，汇总风险点和证据链。
- `ReadmeRiskScanner`：检查 README 中的关键表述是否能在项目文件中找到证据。

系统会结合文件路径、文件类型和内容关键词判断证据，例如 `pom.xml`、`Dockerfile`、`application.yml`、SQL 文件、JWT 相关代码、Redis 相关代码等。

## 为什么要证据链

只给用户一个评分不够。学生真正需要的是知道“哪些表述能被代码支撑，哪些表述容易被追问”。证据链可以把结论和文件、配置、代码线索关联起来，帮助用户判断简历应该怎么写。

证据链也是这个项目区别于纯 AI 套壳的核心：AI 可以负责总结和表达，但关键判断要尽量回到项目材料本身。

## AI 为什么要降级

AI 服务可能没有配置 Key、调用失败、返回格式不稳定，或者生成内容偏乐观。为了保证核心流程可用，系统把规则扫描作为基础能力。

当 AI 不可用时，系统仍然可以生成规则版报告；当 AI 可用时，AI 只做增强和表达补充。

## ZIP 上传安全怎么做

ZIP 上传做了几类限制：

- 文件大小限制为 10MB。
- 只允许 `.md`、`.xml`、`.yml`、`.yaml`、`.properties`、`.java`、`.sql`、`.json`、`Dockerfile`、`.gitignore` 等文本文件。
- 跳过 `.git`、`target`、`node_modules`、`dist`、`build` 等目录。
- 拦截绝对路径、盘符路径、`..` 路径和空字节。
- 跳过图片、Office 文档、压缩包、可执行文件、音视频等二进制文件。
- 单个文本文件限制为 300KB，避免异常大文件影响解析。

## 异步任务怎么做

前端调用 `POST /api/projects/{projectId}/analyze` 创建任务。后端先写入 `pm_analysis_task`，状态为 `PENDING`，然后通过 `@Async("analysisTaskExecutor")` 启动后台分析。

后台任务会更新进度，执行规则扫描和报告生成。前端通过 `GET /api/tasks/{taskId}` 轮询任务状态。

## Redis 在哪里用

Redis 当前用于缓存异步分析任务进度。任务进度 key 类似 `analysis:task:{taskId}`，过期时间为 2 小时。

如果 Redis 写入或读取失败，任务本身不应该直接失败，因为 MySQL 中仍然保存了任务状态。

## 额度系统怎么设计

注册时创建用户套餐并赠送 3 次分析额度。生成报告前扣除 1 次额度，写入 `pm_credit_log`。如果报告生成失败，并且已经扣除额度，系统会返还额度并记录返还流水。

当前还提供管理员加额接口，用于演示额度运营能力，但没有接入支付。

## Docker 部署怎么讲

Docker Compose 编排了 5 个服务：

- `mysql`：保存业务数据，并通过 init.sql 初始化表结构。
- `redis`：缓存异步任务进度。
- `backend`：Spring Boot 后端。
- `frontend`：构建 Vue 前端静态资源。
- `nginx`：提供统一访问入口，并把 `/api` 转发到后端。

演示时可以强调：这个项目支持本地运行和 Docker Compose 部署，适合作为全栈 MVP 或后端实习项目展示。

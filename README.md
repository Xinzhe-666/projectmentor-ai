# ProjectMentor AI｜AI 项目真实性审计与面试深挖平台

ProjectMentor AI 是一个面向计算机学生和后端实习求职者的项目真实性审计工具，用于识别 README 夸大、AI 回答幻觉、简历风险和面试追问风险。

## 项目背景

越来越多学生会使用 AI 辅助做项目、写 README、整理简历和准备面试，但 AI 生成内容容易带来新的风险：

- README 描述比真实代码实现更“满”。
- 简历描述找不到对应代码、配置或运行证据。
- AI 评价过度鼓励，让用户高估项目成熟度。
- 面试被追问到具体实现时，容易解释不清。

ProjectMentor AI 的定位不是“让 AI 直接打分”，而是先做规则扫描和证据链整理，再用 AI 做表达增强和审计补充。AI 不可用时，系统仍然可以输出规则版报告。

## 核心功能

| 功能 | 状态 | 说明 |
| --- | --- | --- |
| 用户认证 | 已完成 | 支持注册、登录、JWT 鉴权、BCrypt 密码加密 |
| 项目管理 | 已完成 | 支持创建、列表、详情、删除项目 |
| README 保存 | 已完成 | 支持粘贴 README 并保存为项目文件 |
| ZIP 上传解析 | 已完成 | 支持普通项目 ZIP，最大 200MB，解析白名单文本文件并过滤无关目录 |
| 规则扫描 | 已完成 | 基于 README 与项目文件做风险识别 |
| 证据链 | 已完成 | 对关键结论关联文件、配置或代码证据 |
| 审计报告 | 已完成 | 输出评分、风险点、建议和简历改写版本，已支持报告详情页浏览器打印 / 另存为 PDF |
| 只读报告分享 | 已支持 | 审计报告已支持生成随机 Token 只读分享链接，公开页仅展示脱敏后的报告内容 |
| AI 增强报告 | 基础版 | 支持 OpenAI-compatible API；AI 不可用时降级为规则报告 |
| AI 幻觉检测 | 已完成 | 检测 AI 回答中的过度鼓励、缺少证据和简历风险 |
| 项目问答 | MVP | V4.2-4 支持轻量检索增强问答、历史记录、证据可信度、面试讲法、简历风险提示、面试版复制、关键词扩展、文件权重和检索解释，不接入向量数据库 |
| 面试深挖 | 基础版 | 规则版 V1，支持会话、追问、评分、总结，已支持面试复盘打印 / 另存为 PDF |
| 额度系统 | 已完成 | 注册赠送额度，报告生成消耗额度，失败返还并记录流水 |
| 管理员后台 | MVP | V4.3-3 支持只读数据看板、管理员手动发放额度和反馈管理，通过 `ADMIN_EMAILS` 配置管理员邮箱，不引入复杂 RBAC |
| 国际化与视觉升级 | 已完成 | V4.4-0 支持中文 / English 切换、localStorage 持久化、首页 / 控制台 / 问答 / 后台视觉升级和轻量 CSS 动效 |
| 首页体验收口 | 已完成 | V4.1-10 增加正式产品说明、试用提示、适合人群、核心流程和能力边界 |
| 作者支持入口 | 轻量版 | “请作者喝咖啡”仅展示本地二维码，自愿支持，不影响任何功能使用，不是支付系统 |
| 反馈入口 | 已完成 | 登录用户可站内提交反馈，管理员可筛选和更新反馈状态；保留 GitHub Issues 作为备用入口，不做复杂工单系统 |
| 异步任务 | 已完成 | 支持异步分析任务，Redis 缓存任务进度 |
| Docker 部署 | 已完成 | 支持 Docker Compose 启动 MySQL、Redis、后端、前端和 Nginx |

## 产品流程图

```mermaid
flowchart LR
    A[注册登录] --> B[创建项目]
    B --> C[上传 README / ZIP]
    C --> D[项目问答]
    D --> E[规则扫描]
    E --> F[生成报告]
    F --> G[查看证据链]
    G --> H[简历优化]
    H --> I[模拟面试]
```

## 技术架构

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17, Spring Boot 3, MyBatis-Plus, MySQL 8, Redis, JWT, BCrypt, Validation, AOP, `@Async` |
| 前端 | Vue 3, Vite, TypeScript, Element Plus, Pinia, Vue Router, vue-i18n, Axios, ECharts, markdown-it |
| 部署 | Docker Compose, Nginx, MySQL, Redis |
| AI | OpenAI-compatible API，可接 DeepSeek / 豆包 / OpenAI 风格接口 |

AI Key 未配置或调用失败时，系统会自动降级为规则版报告。

## 核心设计亮点

- 不直接相信 README：README 只是待验证材料，系统会结合上传文件做规则扫描。
- 不直接相信 AI 回答：AI 幻觉检测会识别过度鼓励、缺少证据和不适合写入简历的表述。
- 先规则扫描，再 AI 增强：规则扫描负责稳定输出，AI 负责补充表达与总结。
- 项目问答仍是轻量 MVP：只基于已保存文件做关键词扩展、文件权重打分、证据片段抽取和证据充分度评估，不接入 Milvus、向量数据库或 embedding。
- 每个关键结论尽量带证据：通过文件类型、路径、配置和代码片段形成证据链。
- 用户数据按 `userId` 隔离：项目、任务、报告、额度和面试会话都按当前用户校验。
- ZIP 上传安全过滤：支持普通项目 ZIP，最大 200MB，自动过滤 `.git`、`target`、`node_modules`、`dist`、`build` 等目录，只解析核心文本文件；大文件上传可能需要数分钟，请不要刷新页面。
- 异步任务避免接口阻塞：分析任务进入后台执行，前端轮询任务状态。
- 额度流水可追踪：当前只实现额度账户、消耗、返还和流水记录，尚未接入支付系统。
- 管理员后台保持克制边界：通过环境变量配置管理员邮箱，可查看统计和最近记录，也可为指定用户增加额度、筛选反馈和更新反馈状态；额度发放必须写入流水，不返回密码、密钥或项目源码内容。
- 试用边界明确：首页和登录后 Dashboard 均提示不要上传真实商业机密、真实密钥或公司内部代码，AI 结论仅供学习、项目复盘和面试准备参考。
- V4.4-0 增加基础国际化：前端支持 `zh-CN` 与 `en-US`，语言选择持久化到 localStorage；没有保存值时会根据浏览器语言判断，默认中文。
- V4.4-0 优化产品视觉：Landing、Dashboard、Project Q&A 和 Admin Dashboard 更接近正式 AI SaaS 产品体验；动效仅使用轻量 CSS，不引入复杂动画依赖。

## V4.1-10 首页、支持与反馈

V4.1-10 主要做产品体验收口，不新增数据库表，不修改核心审计逻辑，不接入真实支付接口：

- 首页补充产品定位、适合人群、核心流程、当前试用能力和试用版边界。
- 首页和登录后 Header 提供“请作者喝咖啡”入口，仅展示 `/donate/wechat.png` 与 `/donate/alipay.png` 两个本地二维码路径。
- “请作者喝咖啡”是完全自愿支持，不影响任何功能使用，不是支付系统。
- 首页和登录后 Header 提供反馈入口；V4.3-3 已升级为站内提交，并保留复制模板和 GitHub Issues 备用入口。
- 真实收款码图片不提交到 GitHub；本地部署时可在 `frontend/projectmentor-web/public/donate/` 下放置 `wechat.png` 和 `alipay.png`。
- `frontend/projectmentor-web/public/donate/README.md` 可以提交，用于说明本地二维码文件放置方式。

## V4.2 项目问答 MVP

V4.2-1 增加项目详情页的“项目问答”入口，用于围绕当前项目已上传的 README 和 ZIP 解析文本提问。

- 当前不是成熟向量 RAG，不接入 Milvus，不做 embedding，只做轻量关键词检索增强问答。
- 后端会先按当前登录用户校验项目归属，再检索该项目自己的 `pm_project_file` 文件内容。
- 回答会返回文件路径、命中原因和 500 字以内的证据片段。
- AI 可用时，回答必须基于证据组织；证据不足时要明确提示。
- AI 关闭、Key 缺失、调用失败或超时时，接口会返回规则检索到的证据片段，不直接编造答案。
- 新增问答记录表 `pm_project_qa_record`，用于保存问题、回答、AI 是否使用、证据 JSON 和建议追问 JSON。
- 已有数据库需要手动执行 `backend/projectmentor-server/src/main/resources/db/init.sql` 中的 `pm_project_qa_record` 建表 SQL。

V4.2-2 补齐项目问答体验闭环：

- 新增最近问答历史查询，默认返回最近 20 条，按创建时间倒序，问答记录仅用户本人可见。
- 支持逻辑删除自己的某条问答记录，删除校验同时包含 `userId + projectId + recordId`。
- 提问成功后自动刷新历史，清空输入框，并把本次回答展示在顶部。
- 建议追问和快捷问题都以按钮形式填入输入框，避免误触发 AI 调用。
- 每条回答支持复制，复制内容包含问题、回答、证据文件路径和建议追问。
- 证据片段默认收起为 3 到 5 行，可展开或收起，避免长片段撑爆页面。
- AI 不可用时继续展示规则检索结果，并明确提示这不是完整结论。
- 无证据时提示用户先保存 README、上传项目 ZIP，或把问题问得更具体。

V4.2-3 增加项目问答可信度和面试复盘输出：

- 每次回答返回证据可信度等级：强证据、中等证据、弱证据或证据不足。
- `confidenceScore` 表示基于当前上传文件的证据充分度，不代表 AI 回答的绝对正确率。
- 后端根据证据数量、内容命中、路径命中、README / 配置 / 代码文件角色生成证据摘要。
- 新增面试讲法，帮助用户把回答转成真实、克制、可追问的表达。
- 新增简历风险提示，证据不足时不建议写进简历或过度包装。
- 支持一键复制面试版回答，内容包含问题、证据可信度、面试讲法、简历风险、关键证据文件和建议追问。
- 历史记录不新增数据库字段，会基于已保存的证据 JSON 动态计算可信度和复盘字段。

V4.2-4 优化项目问答检索质量和稳定性：

- 增强中文问题、英文技术词、camelCase、snake_case、kebab-case 的关键词提取，并增加登录鉴权、Redis / 缓存、上传、报告、AI、异步、部署、额度等轻量同义词映射。
- 优化 Controller、Service、Config、Interceptor、Filter、Util、Mapper、Entity、DTO / VO、配置文件、部署文件、SQL 和 README 的文件角色权重。
- 降低锁文件、构建产物、压缩产物、大型样式文件和日志类文件的误命中影响，避免 README 或巨大文件因为重复词过多压过真实代码证据。
- 证据 snippet 优先截取关键词、类名、方法名或配置块附近内容，并控制长度，便于用户复盘真实代码证据。
- 命中原因会解释文件路径、内容关键词、技术词和文件角色为什么相关，前端支持复制证据文件路径。
- `confidenceScore` 仍表示证据充分度，不是 AI 正确率；证据较弱时不建议过度写进简历。
- 后续 V4.2-5 或 V4.3 可在反馈足够后评估向量检索升级或管理员后台。

## V4.3-3 站内反馈管理

V4.3-3 在原轻量反馈入口基础上补齐站内提交和管理员处理闭环：

- 新增 `pm_feedback` 表，保存登录用户提交的反馈类型、内容、联系方式、来源页面、状态和管理员备注。
- 登录用户可通过首页或登录后 Header 的“反馈”弹窗提交站内反馈；未登录提交会提示先登录。
- 管理员后台新增“反馈管理”区域，可按类型、状态和关键词筛选反馈，查看详情并更新状态为待处理、处理中、已解决或暂不处理。
- 管理员反馈接口位于 `/api/admin/feedback/**`，继续复用 `ADMIN_EMAILS`、`AuthInterceptor` 和 `AdminInterceptor`。
- 该能力不是复杂工单系统，不做邮件通知、客服聊天、删除反馈或用户数据导出。
- 已有线上数据库不会自动重放 `init.sql`，需要手动执行 `pm_feedback` 建表 SQL。

## V4.4-0 国际化与产品视觉升级

V4.4-0 聚焦公开测试展示和常见用户路径，不修改数据库结构，不修改后端核心业务逻辑：

- 前端新增 `vue-i18n` 基础国际化能力，支持 `zh-CN` 与 `en-US`。
- 语言切换入口位于首页右上角、登录后 Header、登录 / 注册页和公开分享页右上角。
- 语言设置写入 `localStorage`，刷新后保持；没有保存值时优先根据浏览器语言判断。
- 主要覆盖 Landing、登录、注册、Header、Sidebar、Dashboard、项目列表、创建项目、项目详情、报告详情、公开分享报告、模拟面试、AI 幻觉检测、项目问答、反馈弹窗、赞助弹窗和管理员后台主要区块。
- Landing 首页强化 Hero、CTA、核心卖点、试用提示和玻璃感功能卡片，更适合中英文公开测试展示。
- Dashboard 统一欢迎区、统计卡片和功能入口卡片；Project Q&A 强化输入区、快捷问题、证据可信度和证据列表；Admin Dashboard 优化统计卡、额度管理和反馈管理分组。
- 动效使用 `animations.css` 中的页面渐入、卡片 hover、按钮 hover glow、背景光幕慢速漂移和功能卡片 stagger animation，不新增大型动画库。
- 当前仍是 Beta 测试版，英文版用于更友好的公开测试展示；AI 输出仍仅供学习、项目复盘和面试准备参考。

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

## 本地运行方式

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

说明：上述命令保留用于本地或资源充足环境的完整 Compose 构建启动。2 核 2G 轻量服务器可以运行项目，但不适合每次在服务器上构建前端；不建议在服务器执行 `docker compose build frontend`、`docker compose up -d --build`，也不建议在服务器执行 `npm run build`。前端更新推荐在本地构建 `dist`，压缩后上传服务器覆盖静态文件。

访问：

```text
http://localhost
```

更完整的 Docker 说明见 [docs/deploy-docker.md](docs/deploy-docker.md)。

## 轻量服务器更新建议

前端轻量更新流程：

本地 Windows PowerShell：

```powershell
cd C:\Users\LiXin\Desktop\projectmentor-ai\frontend\projectmentor-web
npm.cmd run build
Remove-Item -Force dist.zip -ErrorAction SilentlyContinue
Compress-Archive -Path dist* -DestinationPath dist.zip -Force
scp dist.zip root@服务器IP:/opt/projectmentor-ai/frontend-dist.zip
```

服务器：

```bash
cd /opt/projectmentor-ai
rm -rf /tmp/projectmentor-frontend-dist
mkdir -p /tmp/projectmentor-frontend-dist
unzip -o frontend-dist.zip -d /tmp/projectmentor-frontend-dist
docker exec projectmentor-frontend-assets sh -c "rm -rf /usr/share/nginx/html/*"
docker cp /tmp/projectmentor-frontend-dist/. projectmentor-frontend-assets:/usr/share/nginx/html/
docker compose restart frontend nginx
docker compose ps
```

后端如果修改 Java 代码，低配服务器可以构建但会较慢：

```bash
docker compose build backend
docker compose up -d backend
```

如果服务器构建明显卡顿，后续可升级为 GitHub Actions 构建镜像后再拉取部署。

## Cloudflare Tunnel 临时试用

如果只是想把本机运行的项目临时发给同学试用，可以使用 Cloudflare Tunnel 暴露前端开发服务。该方式适合临时演示，不适合长期正式运营。

说明见 [docs/cloudflare-tunnel-demo.md](docs/cloudflare-tunnel-demo.md)。

## 部署与试用文档

- Docker Compose 本地部署：[docs/deploy-docker.md](docs/deploy-docker.md)
- Cloudflare Tunnel 临时试用：[docs/cloudflare-tunnel-demo.md](docs/cloudflare-tunnel-demo.md)
- VPS 试用版部署：[docs/server-deploy-vps.md](docs/server-deploy-vps.md)

## 环境变量说明

| 变量 | 说明 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码，Docker Compose 启动时必填 |
| `JWT_SECRET` | JWT 签名密钥，请使用足够长的随机字符串 |
| `ADMIN_EMAILS` | 管理员邮箱白名单，英文逗号分隔；配置后需要重启 backend 生效 |
| `AI_ENABLED` | 是否启用 AI 增强；未配置 Key 或调用失败时仍会 fallback 到规则版报告 |
| `AI_BASE_URL` | OpenAI-compatible API 地址，例如 DeepSeek 或其他兼容接口 |
| `AI_API_KEY` | AI 服务密钥，可留空；留空时使用规则版报告 |
| `AI_MODEL` | AI 模型名称，例如 `deepseek-chat` |
| `AI_MAX_PROMPT_CHARS` | AI 提示词最大字符数，默认 `12000` |
| `AI_MAX_RESPONSE_TOKENS` | AI 响应 token 上限，默认 `1600` |

不要提交真实 `.env` 文件或真实密钥。仓库中只保留 `.env.example`。

## 上线安全收口

- MySQL 和 Redis 只在 Docker Compose 内部网络中供后端通过服务名 `mysql`、`redis` 访问，不映射公网端口。
- 生产环境只开放 `22`、`80`、`443`；云防火墙不应开放 `3306`、`6379`。
- `.env` 不提交到仓库。
- `AI_API_KEY`、`JWT_SECRET`、`MYSQL_ROOT_PASSWORD` 必须通过环境变量配置，不要写入代码或公开文档。

## API 模块概览

- `auth`：注册、登录、当前用户、退出登录。
- `projects`：项目创建、列表、详情、删除。
- `files`：README 保存、文件列表、文件详情、删除文件。
- `upload-zip`：上传项目 ZIP，自动过滤无关目录和二进制文件，并解析核心文本文件。
- `scan`：README 风险规则扫描与证据链生成。
- `reports`：生成审计报告、查看项目报告列表、查看报告详情。
- `share`：为审计报告生成、刷新、关闭只读分享链接，并通过随机分享 Token 访问脱敏公开报告。
- `analyze tasks`：启动异步分析任务、查询任务进度。
- `hallucination`：检测 AI 回答幻觉和简历风险。
- `interview`：启动模拟面试、提交回答、查看会话、结束面试。
- `credits`：查看额度、额度流水。
- `feedback`：登录用户提交站内反馈。
- `admin`：管理员身份检查、系统统计、最近用户 / 项目 / 报告 / 问答只读列表、额度发放和反馈管理。

更完整的接口说明见 [docs/api-overview.md](docs/api-overview.md)。

## 演示流程

1. 注册新用户，系统赠送初始分析额度。
2. 登录后进入项目列表。
3. 创建项目，填写名称、技术栈和项目描述。
4. 上传普通项目 ZIP，系统自动过滤 `.git`、`target`、`node_modules`、`dist`、`build` 等目录，只解析 README、配置文件、Java 文件、SQL 文件等核心文本文件；大 ZIP 上传较慢是正常现象，建议删除 `node_modules`、`target`、`.git` 后再压缩。
5. 执行规则扫描，查看 README 风险点和证据链。
6. 启动异步分析任务，等待报告生成。
7. 查看项目审计报告，包括评分、风险点、证据链、建议和简历写法。
8. 在报告详情页生成只读分享链接，复制后可在未登录窗口打开；分享页不暴露用户邮箱、用户 ID、AI Key、AI 调用日志、额度流水或原始项目源码内容。
9. 粘贴一段 AI 对项目的评价，执行 AI 幻觉检测。
10. 启动模拟面试，回答追问并查看反馈。
11. 点击“反馈”提交站内反馈；管理员可在 `/admin` 的“反馈管理”中查看和更新状态。

更完整的演示说明见 [docs/demo-guide.md](docs/demo-guide.md)。

## 简历与面试准备

📚 面试准备与简历写法：请参考 [docs/interview-preparation.md](docs/interview-preparation.md)。

## 当前限制

- AI Key 未配置时使用规则版报告，不会调用外部 AI 服务。
- 面试深挖当前是规则版 V1，追问逻辑还需要继续扩展。
- 管理员后台当前不做复杂 RBAC，不提供用户删除、封号、扣减额度或数据导出能力；V4.3-3 支持管理员手动增加额度和反馈状态管理，不是支付系统或工单系统。
- 尚未接入真实支付，当前只有额度账户和流水；“请作者喝咖啡”只是自愿二维码支持入口。
- ZIP 上传当前最大支持 200MB，会自动过滤常见依赖、构建和 IDE 目录，只解析核心文本文件，不保存二进制文件。
- 审计报告只读分享当前为基础版能力，不包含访问统计和密码保护。
- 反馈入口支持登录用户站内提交，并保留模板复制和 GitHub Issues 备用入口；当前不是正式工单系统。
- 还需要真实用户测试，用于调整规则、文案和演示流程。

## 后续路线

- V4.0：当前可试用版本。
- V4.1：PDF 导出 / 分享报告 / 首页体验收口（已支持报告页和面试复盘通过浏览器打印窗口另存为 PDF，已支持审计报告只读分享链接；V4.1-10 已补充首页产品说明、试用提示、自愿支持二维码入口和轻量反馈入口；正式后端 PDF 导出可作为后续增强）。
- V4.2：项目问答。V4.2-4 已完成轻量检索增强问答、问答历史、证据可信度、面试讲法、简历风险提示、面试版复制、关键词扩展、文件权重、snippet 优化和检索解释；后续 V4.2-5 或 V4.3 可评估向量检索升级或管理员后台。
- V4.3：管理员后台 / 用户反馈。V4.3-3 已完成管理员只读数据看板、额度发放和反馈管理。
- V4.4：更正式的部署和用户体系。

详细路线见 [docs/roadmap.md](docs/roadmap.md)。

## 更多文档

- [项目架构说明](docs/project-architecture.md)
- [API 概览](docs/api-overview.md)
- [演示指南](docs/demo-guide.md)
- [Docker 部署说明](docs/deploy-docker.md)
- [Cloudflare Tunnel 临时试用](docs/cloudflare-tunnel-demo.md)
- [VPS 试用版部署](docs/server-deploy-vps.md)
- [面试准备与简历写法](docs/interview-preparation.md)
- [Roadmap](docs/roadmap.md)

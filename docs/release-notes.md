# V4.8-0 中文版本记录与注册邮箱验证码

本版本把 release notes 从英文主线调整为中文主线，并新增注册邮箱验证码能力。

新增：

- 注册页支持发送邮箱验证码。
- 注册接口新增验证码校验，验证码正确后才创建账号并赠送 10 credits。
- 验证码默认存 Redis，Redis 不可用时使用进程内内存兜底。
- 增加验证码发送冷却、单邮箱小时限制和单 IP 小时限制，避免被刷邮件。
- 邮件服务通过环境变量配置，仓库不保存真实 SMTP 密码。
- release notes 改为中文主线，方便面向中文老师、同学、面试官和内测用户展示。

边界：

- 不新增数据库表。
- 不修改 credits 扣费标准。
- 不修改登录、JWT、管理员权限和 AI 调用逻辑。
- 邮箱验证码只用于注册阶段，不做找回密码、换绑邮箱或二次登录验证。
- `EMAIL_VERIFICATION_ENABLED=false` 时验证码校验关闭，便于本地开发；生产环境配置 SMTP 后再开启。

# V4.7.1 生产域名 CORS 修复

本版本修复正式域名访问后端时登录、注册请求出现 403 的问题。

根因是线上前端已经切换到正式域名，但后端 CORS 白名单仍偏向本地开发和早期部署地址。浏览器在发送登录、注册等 `POST` 请求前会先发起 `OPTIONS` 预检；如果后端没有把正式域名加入 allowed origins，预检会被拒绝，用户看到的就是登录或注册 403。

修复方式：

- 在 `app.cors.allowed-origins` 中补齐正式域名、`www` 域名、HTTP 兼容入口、服务器 IP、本地 Vite 和本地调试端口。
- 保持 `/api/auth/login`、`/api/auth/register`、`/api/health` 等公开接口无需登录。
- 鉴权拦截器继续放行所有 `OPTIONS` 预检请求，避免浏览器预检被误判为未登录 API 调用。
- 增加 CORS 测试，覆盖生产域名登录 / 注册预检、未知域名拒绝、Authorization 请求头放行。

边界：

- 不改变 JWT 鉴权、管理员权限、用户注册限流和 credits 逻辑。
- 不放开任意来源，生产环境仍使用显式白名单。

# V4.7 Public Preview 稳定化

ProjectMentor AI 进入更稳定的 Public Preview 阶段。本版本不新增重后端业务逻辑，不改数据库结构，不接入支付，也不更换 AI 服务商，重点放在公开展示一致性、正式域名准备、产品边界说明和演示路径可靠性上。

新增：

- 统一围绕项目真实性审计、Claim-Evidence Matrix 和面试深挖来表达 Public Preview 定位。
- 明确官网文案和后续截图中的产品边界，避免把项目包装成企业级代码审计、安全审计或权威认证系统。
- 增加已知限制文档，说明 PMAI 当前是面向学习、项目复盘、简历表达检查和面试准备的 SaaS MVP。
- 梳理面向老师、面试官、早期用户和公开内测用户的演示路径。
- 对齐 README、roadmap、release notes、architecture notes 和 demo guide 中的版本表达。
- 说明规则扫描和基础 Claim-Evidence 检查免费，AI 增强能力消耗 credits，失败会自动返还。
- 稳定项目问答边界：当前是轻量证据检索，不是成熟向量数据库 RAG。
- 稳定 credits 边界：当前是公开预览额度系统，不是真实支付或账单系统。

边界：

- 不新增后端 API、数据库字段、第三方依赖、支付系统、埋点、AI provider、API key 或 `.env` 配置。
- 不宣称 PMAI 是企业级代码审计平台、商业安全审计系统、官方项目真实性认证系统或成熟向量 RAG 产品。
- 产品仍是用于学习、项目复盘、简历表达检查和面试准备的 Public Preview SaaS MVP。

# V4.6 Public Preview Launch Pack

本版本补齐公开预览发布所需的站点信息、隐私说明、分享页可信表达和部署说明，让 PMAI 更适合被外部用户第一次访问和理解。

新增：

- 静态 SEO metadata、浏览器标题、Open Graph、Twitter card、应用名称和 robots 配置。
- 中英文 Public Preview footer，包含 credits 提示、隐私说明、上传警告和轻量 Privacy & Disclaimer 入口。
- 中英文隐私与免责声明，覆盖上传材料、AI 局限、credits、敏感数据和公开报告边界。
- 更可信的公开报告展示，包括证据生成上下文、规则版与 AI 增强状态，以及“非官方认证”的明确提示。
- Nginx 安全响应头，不改变现有 `/api` 代理、history fallback 和上传限制。
- VPS 与 Docker 部署说明，覆盖 DNS、`server_name`、HTTPS 证书、反向代理行为和后续 sitemap 准备。

边界：

- 不修改后端 API、数据库字段、依赖、支付系统、埋点、外部图片、AI provider、credit-cost 规则、API key、`.env.development` 或 LICENSE。
- 在正式 canonical public domain 配置前不发布 sitemap，避免出现占位或 IP 地址 URL。

# V4.5-5 前端体验、Onboarding 与演示流程优化

本版本集中优化公开体验、空状态、演示路径和文档说明，让新用户更容易理解 PMAI 能做什么、哪些能力免费、哪些 AI 能力会消耗 credits。

新增：

- 更清晰的中英文 Landing page，围绕项目真实性审计和面试深挖表达价值。
- 三张核心价值卡片、四步工作流、Rule vs AI 对比、适用人群卡片和八步演示路径。
- 零项目状态下的 onboarding 卡片，提供项目创建、credit 规则和 demo guide 入口。
- 项目创建指导和只读示例，示例不会写入样例数据。
- 报告阅读指引，以及 `SUPPORTED`、`PARTIAL`、`DOC_ONLY`、`NO_EVIDENCE`、`RISKY` 的解释。
- 更清晰的额度中心，说明 10-credit 注册赠送、免费能力、AI 成本、失败退款和 beta 额度边界。
- 项目、报告、面试、额度、Claim-Evidence、AI enhancement 和 admin 页面空状态。
- 管理员用量卡片、额度管理间距、表格可读性和空数据操作优化。
- README、demo guide、roadmap、interview preparation 和 architecture 文档更新。

边界：

- 不修改后端 API、数据库字段、依赖、支付系统、AI provider、credit-cost 规则、外部图片、API key 或 `.env.development`。
- 规则扫描继续免费；AI 审计、解释、问答、幻觉检测和模拟面试继续按既有额度扣减。

# V4.5-4 AI 成本监控、管理员额度操作与注册防刷

本版本补齐管理员视角的额度与 AI 用量管理，并加入基础注册限流，降低公开测试期间被批量注册消耗资源的风险。

新增：

- 管理员额度用户分页，支持搜索、余额和累计 credit 指标。
- 用户额度流水分页，支持类型、模块和时间筛选。
- 管理员发放与扣除额度接口，分别写入 `ADMIN_GRANT` 和 `ADMIN_DEDUCT` 审计流水。
- 基于既有 credit logs 的 AI 用量总览，包含今日 / 累计调用、消耗、退款、模块排行、用户排行和最近 AI 交易。
- 中英文管理员 UI，支持 AI 用量、额度发放、扣除和用户流水查看，写操作带确认弹窗。
- 同 IP 注册限制：每小时最多成功注册 3 个账号，每天最多 10 个账号。
- 注册计数默认使用 Redis，Redis 不可用时使用进程内内存兜底。
- 覆盖管理员授权、额度调整、AI 用量聚合、注册限流和 10-credit 注册赠送的单元测试。

边界：

- 不接入支付系统，不新增第三方依赖、数据库表、AI provider、API key 或 `.env.development`。
- 规则扫描、上传、项目管理、Dashboard、历史查看和公开分享继续免费。

# V4.5-3 全量 AI credits 扣费闭环

本版本把所有 OpenAI-compatible LLM 入口统一纳入 credits 扣费和失败退款逻辑，避免不同 AI 功能之间出现成本口径不一致。

新增：

- 统一费用：AI 审计报告 2、Claim-Evidence enhancement 2、项目问答 1、幻觉检测 1、模拟面试 2、独立简历优化 1 credit。
- 所有付费 AI 入口在调用前检查并预留 credits。
- AI 调用失败、返回不可用结果或业务结果保存失败时自动退款。
- 项目问答、幻觉检测和面试启动在 AI 不可用时回退到规则版，不扣费。
- 所有 AI 操作显示预计成本和确认提示，中英文文案同步。
- 额度中心文档补齐完整 AI 成本表和新操作类型。
- 单元测试覆盖 LLM mock、余额不足、退款、注册赠送和免费历史访问。

边界：

- 规则扫描、上传解析、项目管理、Dashboard、历史查看、公开报告分享和管理员反馈继续免费。
- 不修改数据库字段、AI provider、依赖、支付系统、API key 或 `.env.development`。

# V4.5-2 AI Claim-Evidence 增强与扣费常量统一

本版本为 Claim-Evidence Matrix 增加 AI 解读能力，并把相关扣费常量集中管理。

新增：

- 新用户注册后通过既有 `REGISTER_GIFT` credit log 获得 10 credits。
- 增加共享 AI credit-cost 常量，包括 `AI_CLAIM_EVIDENCE = 2`。
- 增加 `POST /api/reports/{reportId}/claim-evidence/ai-enhance`，用户可对自己的报告进行 Claim-Evidence AI 增强。
- 报告详情页增加 2-credit 确认、加载状态、成功刷新和中英文 UI 文案。
- AI 增强结果保存到 `pm_analysis_report.claim_evidence` JSON，不新增数据库列。
- AI 调用失败或增强结果保存失败时自动退款。
- 已保存的 AI 增强内容可在私有报告和公开报告中渲染，公开页不暴露额度、AI 日志或源码。

边界：

- 规则扫描、上传解析、历史查看和 Dashboard 浏览继续免费。
- 不新增 AI provider、向量数据库、外部依赖或 `.env.development` 配置。
- AI enhancement 只解释结构化 Claim-Evidence 数据，不重新扫描完整仓库，也不编造实现细节。

# V4.5-1 Claim-Evidence 审计引擎

本版本增加规则版 Claim-Evidence 审计能力，把项目描述和源码证据之间的关系显式展示出来，方便用户复盘项目真实性和面试追问风险。

新增：

- 从项目描述、技术栈和 README 中按规则抽取 claim。
- 支持认证、数据库、缓存、AI、项目问答、上传、报告、面试、管理员、credits、部署、前端、安全、性能和产品能力等 claim 类别。
- 在源码、配置、SQL、Docker、Nginx、前端文件和运维脚本中匹配证据。
- Claim 状态包括 `SUPPORTED`、`PARTIAL`、`DOC_ONLY`、`NO_EVIDENCE` 和 `RISKY`。
- 私有报告页和公开分享页展示 Claim-Evidence Matrix，支持状态筛选、风险优先排序、证据展开和面试解释复制。
- 证据片段限制 300 字符，并对 secret、password、token、API key、JWT 和数据库凭证做脱敏。
- 增加 `pm_analysis_report.claim_evidence` JSON 字段。

边界：

- Claim extraction 和证据匹配都是规则版，不新增 AI 调用。
- 这不是法律、学术或权威意义上的真实性鉴定。
- Claim 状态和 AI 结果不保证完全准确，需要结合真实项目证据人工复核。
- 功能主要用于学习、项目复盘、简历表达检查和面试准备。
- 不接入向量数据库，不新增复杂 NLP 依赖，不改变 credit 规则。

# V4.4 Public Preview 基础版

本版本是 ProjectMentor AI 的公开预览基础版，形成从项目上传、规则扫描、AI 审计、面试准备到公开分享的 MVP 闭环。

包含能力：

- 基于 README、源码、配置、部署文件和项目证据的项目真实性审计。
- 规则扫描与证据链生成。
- AI 增强审计报告，并在 AI 不可用时回退规则版。
- 对夸大或无证据项目描述的 AI 幻觉检测。
- 基于轻量证据检索的项目问答。
- 模拟面试会话，支持进度、跳过、结束、评分和复盘。
- 报告历史和面试历史。
- 基于真实用户数据的 Dashboard。
- 额度账户、额度流水、管理员额度发放和反馈管理。
- 中英文国际化。
- 大项目 ZIP 上传优化，最大支持 800MB，并保持严格解析边界。
- Docker Compose 与 Nginx 部署。
- MySQL 备份、恢复和生产检查脚本。
- 针对常见扫描路径的 Nginx 安全加固。
- Source-Available / All Rights Reserved 许可证说明。

已知边界：

- 这是个人 SaaS MVP 和 Public Preview，不是企业级代码审计平台。
- AI 结果仅用于学习、项目复盘、简历准备和面试练习。
- 项目问答当前使用轻量证据检索，不是向量数据库 RAG。
- 未接入支付系统。
- 生产使用、商业使用、机构使用或再分发需要作者明确书面授权。

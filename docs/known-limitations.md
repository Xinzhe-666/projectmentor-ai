# Known Limitations / 已知边界

ProjectMentor AI is a public-preview project authenticity audit and interview deep-dive platform. It helps users review project claims against uploaded materials, but it does not replace human judgment, official review, or careful interview preparation.

## PMAI Can Help With / PMAI 可以帮助做什么

- 检查 README、项目描述、技术栈和上传源码之间是否有证据支撑。
- 识别风险主张、过度表达和缺少证据的描述。
- 生成 Claim-Evidence Matrix，把项目主张和代码、配置、部署或文档证据对应起来。
- 在 AI 不可用时输出规则版报告，保留基础证据扫描结果。
- 可选使用 AI 增强报告、项目问答、幻觉检测和模拟面试。
- 帮助用户准备更真实、更克制、更能经得起追问的面试表达。

## PMAI Is Not / PMAI 不是什么

- 不是企业级代码审计平台。
- 不是商业安全审计系统。
- 不是法律、学术或官方真实性认证系统。
- 不是成熟向量数据库 RAG 系统。
- 不是支付或计费平台。
- 不保证项目绝对真实，也不保证一定能通过面试。
- 不鼓励包装、夸大或伪造项目经历。

## AI Limitations / AI 边界

AI 结果可能不完整、不准确，或遗漏关键上下文。用户必须结合真实代码、配置文件、部署文件、运行结果和个人实际贡献进行人工复核。

AI enhancement is an optional interpretation layer. It should be used to improve clarity and interview preparation, not to invent implementation details or turn weak evidence into strong claims.

## Project Q&A Limitations / 项目问答边界

当前项目问答是 lightweight evidence-retrieval MVP，基于关键词提取、同义词扩展、文件角色权重、证据片段和可选 AI 解释，帮助用户定位“某个能力可能在哪里被实现”或“哪些文件可以作为面试证据”。

当前不使用 Milvus、PgVector、embedding-based retrieval、semantic vector search 或成熟 RAG pipeline。项目问答不应被介绍为完整语义检索系统或成熟向量数据库 RAG 产品。

## Credits Limitations / Credits 边界

Credits 是 public-preview usage credits，用于控制 AI 功能消耗、减少不必要模型调用，并记录 AI 调用失败后的返还行为。

Credits 不是真实支付系统，不代表正式计费、充值、订阅、账单、发票或商业支付能力。

## Upload Limitations / 上传边界

请不要上传真实商业机密、公司内部代码、API Key、Token、密码、数据库凭据、内部部署配置、敏感个人信息或任何不适合公开审查的材料。

如果需要演示，建议使用个人项目、课程项目、开源示例或已经脱敏的源码核心包。

## Human Review Is Required / 必须人工复核

`SUPPORTED` 只表示当前上传材料中找到了相对明确的证据，不代表官方认证。`NO_EVIDENCE` 或 `RISKY` 也只表示当前上传材料不足或表达存在风险，不等于项目一定不真实。

所有 Claim-Evidence 状态、AI 解读、问答结果、幻觉检测和模拟面试反馈，都需要结合真实项目材料与个人贡献进行人工复核。

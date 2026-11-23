# ProjectMentor AI · 项目证据核对与面试深挖

[![CI](https://github.com/Xinzhe-666/projectmentor-ai/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Xinzhe-666/projectmentor-ai/actions/workflows/ci.yml) · [公开预览](https://www.projectmentorai.com/)

PMAI 帮开发者把 README 和项目描述中的主张，对照已上传的源码、配置、SQL 和部署文件逐项核对。它指出哪些表述有材料支撑、哪些仅见于文档、哪些需要人工复核，并把结果用于项目问答、报告和面试追问。

ProjectMentor AI helps developers review project claims against uploaded source code, configuration, and deployment evidence. It is an evidence review assistant, not a verifier of authenticity or authorship.

## 如何体验

1. 创建项目，粘贴 README 或上传一份可公开分享的源码 ZIP。
2. 运行规则扫描，查看 Claim–Evidence Matrix，再打开证据文件核对结论。
3. 就某项实现提问、生成审计报告或进行模拟面试。规则报告免费；可选 AI 功能使用站内额度，符合条件的失败会返还。额度不是真实支付。

Classic 与 Evidence Workbench 是同一产品的两种界面。首页示例使用虚构项目，不是独立验证的客户案例。

> `SUPPORTED` 表示上传材料中找到相关证据，不证明功能在生产运行、系统达到某项性能指标，也不证明个人贡献。规则可能漏判或误判，AI 输出也需复核。请勿上传密钥、内部代码、商业机密或敏感个人信息。

## v7：实现与部署状态

| 能力 | 状态 |
| --- | --- |
| Public Preview | v7 前后端于 2026-11-20 部署；生产使用 `lexical` 和本地分析调度 |
| 并发与一致性 | MySQL 行锁保护额度余额与流水，任务级幂等与唯一约束，报告落库用短事务，本地执行队列有界 |
| RabbitMQ 可靠任务管线 | 已实现和集成验证，生产未启用；包含事务 Outbox、Confirm/Return、手动 ACK、有限重试、DLQ、租约和 fencing |
| Milvus 混合检索 | 已实现和集成验证，生产未启用；Dense + BM25、服务端 RRF、证据重排、MySQL 原文核验，向量侧故障回退 `lexical` |
| 索引发布 | 以 generation/build 隔离未完成索引，校验源文件 revision 后切换 active 指针 |
| 验证边界 | CI 集成测试不等于生产容量或语义效果。尚无正式容量基线和真实 Embedding 质量结论 |

## 技术栈

| 层 | 技术 |
| --- | --- |
| 应用 | Java 17、Spring Boot 3、MyBatis-Plus、JWT、BCrypt |
| 数据 | MySQL 8、Flyway V1–V6、Redis（缓存与进度，不作为业务事实源） |
| AI 与异步 | OpenAI-compatible API；RabbitMQ 与 Milvus 是可选运行组件 |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、i18n |
| 交付 | Docker Compose、Nginx/HTTPS、Maven、GitHub Actions |

## 开发、部署与评测

本地 Java 进程需显式提供独立的 `JWT_SECRET`；生产 Compose 默认使用 `prod` profile，另需配置准确的 HTTPS `CORS_ALLOWED_ORIGINS`。先阅读部署和安全说明，再启动应用。

- [架构与可靠任务](docs/architecture/v7-milvus-concurrency.md) · [API 概览](docs/api-overview.md)
- [Docker 开发与部署](docs/deploy-docker.md) · [v7 切换与回滚](docs/deployment-v7.md) · [生产安全检查](docs/production-security-checklist.md)
- [Claim 判定基线](docs/claim-evaluation.md) · [检索评测](docs/retrieval-evaluation.md) · [容量基线](docs/capacity-baseline.md)
- [发布历史](docs/release-notes.md) · [已知边界](docs/known-limitations.md) · [隐私与使用声明](docs/privacy-and-disclaimer.md)

## 许可与品牌

Copyright © 2026 李鑫哲. All rights reserved. 本仓库为源码可查看（Source-Available），不采用开源许可证。使用权限及限制以 [LICENSE](LICENSE) 为准；品牌使用与标识记录见 [BRAND.md](BRAND.md)。

# Claim 判定基线

[标注集](../evaluation/claim-labels.tsv)包含 50 条人工编写的主张，覆盖 `SUPPORTED`、`PARTIAL`、`DOC_ONLY`、`NO_EVIDENCE`、`RISKY`。输入来自三个虚构项目；每条包含预期状态和理由，标注不取自规则引擎。部分用例只上传 README。另有刻意相近的反例，例如只识别 Bearer 前缀不等于已经验证 JWT 签名，有取消窗口配置不等于代码执行了限制。

从 `backend/projectmentor-server` 运行：

```bash
bash mvnw -B -Dtest=ClaimEvaluationTests test
```

Windows 可改为 `mvnw.cmd -B "-Dtest=ClaimEvaluationTests" test`。完整报告在 `target/claim-evaluation.md`，CI 的 backend 测试日志打印摘要。指标包括混淆矩阵、总体准确率、逐类 Precision/Recall/FPR（逐类 FPR 分母是所有不属于该类的样本）；没有预测为某类时，Precision 为 `n/a`。

这测试的是实际 Java 匹配器，但给定了人工类别，不测自动主张抽取、分类或 AI 端到端准确率。它只建立可复现基线，错误样本保留，不用这 50 条给自己设一个通过线；策略修改后另加独立保留集复测，尤其关注 `SUPPORTED` 的误报。

## 下一次用户验收

邀请 5–10 位自愿参与的求职者，各用一个可公开的项目走完「上传 → 证据矩阵 → 打开一条证据 → 修改一条项目描述」。只记录是否完成、是否找到可用证据、最明显的误判，以及是否有实际复用意愿；不收集原始简历和私有源码。这是用户验证计划，不是已完成的试用数据，也不是启用 Hybrid 的充分条件。

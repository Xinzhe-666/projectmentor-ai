package com.xinzhe.projectmentor.hallucination.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HallucinationIssueVO {

    /**
     * 风险等级：LOW / MEDIUM / HIGH
     */
    private String riskLevel;

    /**
     * 问题类型：
     * OVER_ENCOURAGEMENT 过度鼓励
     * MISSING_EVIDENCE 缺少证据
     * TECH_OVERCLAIM 技术夸大
     * RESUME_RISK 简历风险
     * UNSUITABLE_SUGGESTION 不适合当前阶段的建议
     */
    private String issueType;

    /**
     * 命中的关键词或表述
     */
    private String matchedText;

    /**
     * 问题说明
     */
    private String message;

    /**
     * 证据说明
     */
    private String evidence;

    /**
     * 修改建议
     */
    private String suggestion;
}
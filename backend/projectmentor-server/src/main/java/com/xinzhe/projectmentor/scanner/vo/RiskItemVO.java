package com.xinzhe.projectmentor.scanner.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiskItemVO {

    /**
     * 风险等级：LOW / MEDIUM / HIGH
     */
    private String riskLevel;

    /**
     * 风险类型，例如 README_OVERCLAIM / MISSING_EVIDENCE
     */
    private String riskType;

    /**
     * 命中的关键词，例如 高并发 / 微服务 / RAG
     */
    private String keyword;

    /**
     * 风险来源文件
     */
    private String sourceFile;

    /**
     * 风险说明
     */
    private String message;

    /**
     * 当前证据情况
     */
    private String evidence;

    /**
     * 修改建议
     */
    private String suggestion;
}
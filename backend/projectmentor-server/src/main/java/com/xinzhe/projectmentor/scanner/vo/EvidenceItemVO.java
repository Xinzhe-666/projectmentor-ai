package com.xinzhe.projectmentor.scanner.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvidenceItemVO {

    /**
     * 结论，例如：项目存在 README 文件
     */
    private String conclusion;

    /**
     * 证据来源文件，例如：README.md / pom.xml
     */
    private String sourceFile;

    /**
     * 具体依据说明
     */
    private String detail;

    /**
     * 风险等级：INFO / LOW / MEDIUM / HIGH
     */
    private String riskLevel;
}
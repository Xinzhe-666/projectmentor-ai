package com.xinzhe.projectmentor.qa.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectQaEvidenceVO {

    private String filePath;

    private String reason;

    private String snippet;
}

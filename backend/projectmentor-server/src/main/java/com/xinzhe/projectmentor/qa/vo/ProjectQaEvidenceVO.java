package com.xinzhe.projectmentor.qa.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectQaEvidenceVO {

    private String filePath;

    private String reason;

    private String snippet;
}

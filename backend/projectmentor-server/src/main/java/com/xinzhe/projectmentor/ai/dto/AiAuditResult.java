package com.xinzhe.projectmentor.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiAuditResult {

    private String summary;

    private String strengths;

    private String weaknesses;

    private String suggestions;

    private String resumeBasic;

    private String resumeStandard;

    private String resumeAdvanced;
}
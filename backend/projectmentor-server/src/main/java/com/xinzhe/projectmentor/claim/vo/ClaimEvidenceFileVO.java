package com.xinzhe.projectmentor.claim.vo;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEvidenceFileVO {

    private Long fileId;

    private String filePath;

    private String fileType;

    private String evidenceLevel;

    private List<String> matchedKeywords;

    private String snippet;

    private String reason;
}

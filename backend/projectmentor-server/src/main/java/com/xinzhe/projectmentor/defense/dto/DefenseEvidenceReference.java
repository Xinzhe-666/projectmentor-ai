package com.xinzhe.projectmentor.defense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefenseEvidenceReference {

    private Long fileId;

    private String filePath;

    private String evidenceLevel;

    private String snippet;

    private String reason;
}

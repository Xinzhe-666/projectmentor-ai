package com.xinzhe.projectmentor.qa.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectQaEvidenceVO {

    private String filePath;

    private String reason;

    private String snippet;

    private Integer startLine;

    private Integer endLine;

    private String chunkType;

    private String language;

    private List<String> matchedTerms;

    private String retrievalMode;

    private String retrievalExplanation;

    private Integer denseRank;

    private Float denseScore;

    private Integer bm25Rank;

    private Float bm25Score;

    private Integer rrfRank;

    private Double evidenceBoost;

    private Integer finalRank;
}

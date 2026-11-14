package com.xinzhe.projectmentor.indexing.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VectorIndexStatusVO {
    private Long projectId;
    private String status;
    private String displayStatus;
    private Long activeGeneration;
    private Integer chunkCount;
    private Integer indexedFileCount;
    private String embeddingModel;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime lastIndexedAt;
    private Long activeTaskId;
    private Integer progress;
}

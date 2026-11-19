package com.xinzhe.projectmentor.indexing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_project_vector_index")
public class ProjectVectorIndex {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private Long userId;
    private String status;
    private Long activeGeneration;
    private String activeBuildId;
    private Long pendingGeneration;
    private String pendingBuildId;
    private Long sourceRevision;
    private Boolean rescheduleRequired;
    private String collectionName;
    private String embeddingProvider;
    private String embeddingModel;
    private Integer embeddingDimension;
    private Integer chunkCount;
    private Integer indexedFileCount;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime lastIndexedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

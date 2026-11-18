package com.xinzhe.projectmentor.indexing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_vector_file_manifest")
public class VectorFileManifest {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private Long userId;
    private Long fileId;
    private String filePath;
    private String contentHash;
    private Long generation;
    private String buildId;
    private String embeddingModel;
    private Integer chunkCount;
    private LocalDateTime indexedAt;
}

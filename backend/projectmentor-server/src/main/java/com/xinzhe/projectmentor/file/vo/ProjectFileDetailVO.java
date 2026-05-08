package com.xinzhe.projectmentor.file.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectFileDetailVO {

    private Long id;

    private Long projectId;

    private String filePath;

    private String fileType;

    private String content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
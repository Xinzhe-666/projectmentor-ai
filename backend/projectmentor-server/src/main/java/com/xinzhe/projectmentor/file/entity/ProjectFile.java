package com.xinzhe.projectmentor.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_project_file")
public class ProjectFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String filePath;

    private String fileType;

    private String content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
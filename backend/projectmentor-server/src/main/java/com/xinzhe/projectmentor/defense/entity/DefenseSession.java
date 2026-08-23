package com.xinzhe.projectmentor.defense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_defense_session")
public class DefenseSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long reportId;

    private String mode;

    private String status;

    private Integer questionCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

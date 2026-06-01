package com.xinzhe.projectmentor.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String contact;

    private String type;

    private String content;

    private String pageUrl;

    private String status;

    private String adminNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}

package com.xinzhe.projectmentor.feedback.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminFeedbackVO {

    private Long id;

    private Long userId;

    private String userEmail;

    private String type;

    private String content;

    private String contact;

    private String pageUrl;

    private String status;

    private String adminNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

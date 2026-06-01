package com.xinzhe.projectmentor.feedback.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FeedbackVO {

    private Long id;

    private String type;

    private String status;

    private LocalDateTime createTime;
}

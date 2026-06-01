package com.xinzhe.projectmentor.feedback.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminFeedbackPageVO {

    private List<AdminFeedbackVO> records;

    private Long total;

    private Integer page;

    private Integer size;
}

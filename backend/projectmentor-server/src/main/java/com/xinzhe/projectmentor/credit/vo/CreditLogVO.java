package com.xinzhe.projectmentor.credit.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreditLogVO {

    private Long id;

    private Long userId;

    private Integer changeAmount;

    private Integer beforeAmount;

    private Integer afterAmount;

    private String operationType;

    private Long businessId;

    private String remark;

    private LocalDateTime createTime;
}
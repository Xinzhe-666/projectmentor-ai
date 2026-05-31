package com.xinzhe.projectmentor.credit.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminCreditUserVO {

    private Long userId;

    private String email;

    private String nickname;

    private Integer creditBalance;

    private LocalDateTime createTime;
}

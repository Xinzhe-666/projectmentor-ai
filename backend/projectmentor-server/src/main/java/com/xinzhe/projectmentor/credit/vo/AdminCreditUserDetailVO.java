package com.xinzhe.projectmentor.credit.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminCreditUserDetailVO {

    private Long userId;

    private String email;

    private String nickname;

    private Integer creditBalance;

    private List<AdminCreditTransactionVO> recentTransactions;
}

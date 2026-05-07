package com.xinzhe.projectmentor.credit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_credit_log")
public class CreditLog {

    @TableId(type = IdType.AUTO)
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
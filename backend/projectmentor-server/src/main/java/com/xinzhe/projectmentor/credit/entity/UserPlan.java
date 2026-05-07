package com.xinzhe.projectmentor.credit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_user_plan")
public class UserPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String planType;

    private Integer remainingCredits;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
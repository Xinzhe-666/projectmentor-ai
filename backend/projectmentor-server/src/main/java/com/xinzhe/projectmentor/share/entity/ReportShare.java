package com.xinzhe.projectmentor.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_report_share")
public class ReportShare {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long reportId;

    private String shareToken;

    private Integer enabled;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

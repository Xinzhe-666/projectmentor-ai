package com.xinzhe.projectmentor.share.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportShareVO {

    private Long reportId;

    private String shareToken;

    private String shareUrl;

    private Boolean enabled;

    private LocalDateTime expireTime;
}

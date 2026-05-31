package com.xinzhe.projectmentor.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminRecentUserVO {

    private Long id;

    private String email;

    private String nickname;

    private LocalDateTime createTime;
}

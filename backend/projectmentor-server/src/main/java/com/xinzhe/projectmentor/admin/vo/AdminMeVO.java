package com.xinzhe.projectmentor.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminMeVO {

    private Boolean admin;

    private Long userId;

    private String email;
}

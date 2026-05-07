package com.xinzhe.projectmentor.auth.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;

    private UserInfoVO userInfo;

    private Integer remainingCredits;
}
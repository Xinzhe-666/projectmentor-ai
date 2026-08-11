package com.xinzhe.projectmentor.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "projectmentor.email-verification")
public class EmailVerificationProperties {

    private boolean enabled = false;

    private String from = "";

    private String subject = "ProjectMentor AI 邮箱验证码";

    private int codeTtlMinutes = 10;

    private int sendCooldownSeconds = 60;

    private int hourlyLimitPerEmail = 5;

    private int hourlyLimitPerIp = 20;

    private int maxVerifyAttempts = 5;
}

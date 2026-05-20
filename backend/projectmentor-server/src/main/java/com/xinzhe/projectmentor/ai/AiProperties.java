package com.xinzhe.projectmentor.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "projectmentor.ai")
public class AiProperties {

    private String baseUrl;

    private String apiKey;

    private String model;

    private Boolean enabled = true;

    private Integer maxPromptChars = 12000;

    private Integer maxResponseTokens = 1600;

    private Integer timeoutSeconds;
}

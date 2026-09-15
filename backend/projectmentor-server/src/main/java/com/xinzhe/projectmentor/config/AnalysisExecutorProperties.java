package com.xinzhe.projectmentor.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "projectmentor.analysis.executor")
public class AnalysisExecutorProperties {

    @Min(1)
    private int corePoolSize = 2;

    @Min(1)
    private int maxPoolSize = 4;

    @Min(0)
    private int queueCapacity = 50;

    @Min(0)
    private int keepAliveSeconds = 60;

    @Min(0)
    private int awaitTerminationSeconds = 30;
}

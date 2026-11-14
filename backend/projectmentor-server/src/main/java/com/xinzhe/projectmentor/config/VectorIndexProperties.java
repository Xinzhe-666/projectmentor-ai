package com.xinzhe.projectmentor.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "projectmentor.vector-index")
public class VectorIndexProperties {
    public enum DispatchMode { LOCAL, RABBIT }
    private DispatchMode dispatchMode = DispatchMode.LOCAL;
    @Min(20) @Max(1000)
    private int chunkMaxLines = 160;
    @Min(0) @Max(100)
    private int chunkOverlapLines = 20;
    @Min(512) @Max(100000)
    private int chunkMaxChars = 12000;
    @Min(1) @Max(10)
    private int maximumAttempts = 4;
    @Min(5) @Max(3600)
    private int leaseSeconds = 120;
    @Min(1) @Max(600)
    private int heartbeatSeconds = 30;
    @Min(1) @Max(32)
    private int consumerConcurrency = 2;
    @Min(1) @Max(100)
    private int prefetch = 1;
    private String mainExchange = "pmai.index.v1";
    private String mainQueue = "pmai.index.execute.v1";
    private String mainRoutingKey = "index.execute.v1";
    private String retryExchange = "pmai.index.retry.v1";
    private String deadLetterExchange = "pmai.index.dlx.v1";
    private String deadLetterQueue = "pmai.index.dead.v1";
    private String deadLetterRoutingKey = "index.dead.v1";
    @Min(100) private long outboxPollIntervalMillis = 1000;
    @Min(1) @Max(500) private int outboxBatchSize = 50;
    @Min(1) private int outboxClaimLeaseSeconds = 30;
    @Min(1) @Max(20) private int outboxMaximumAttempts = 8;
    @Min(100) private long publisherConfirmTimeoutMillis = 5000;
    private List<Long> retryTtlSeconds = new ArrayList<>(List.of(10L, 60L, 300L));
}

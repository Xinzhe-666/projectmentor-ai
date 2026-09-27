package com.xinzhe.projectmentor.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "projectmentor.analysis")
public class AnalysisPipelineProperties {

    public enum DispatchMode {
        LOCAL,
        RABBIT
    }

    public enum QueueType {
        QUORUM,
        CLASSIC
    }

    private DispatchMode dispatchMode = DispatchMode.LOCAL;

    @Valid
    private Rabbit rabbit = new Rabbit();

    public boolean isRabbitMode() {
        return dispatchMode == DispatchMode.RABBIT;
    }

    @AssertTrue(message = "Rabbit analysis pipeline configuration is invalid")
    public boolean isValidConfiguration() {
        if (!isRabbitMode()) {
            return true;
        }
        return rabbit.connection.hasRequiredValues()
                && rabbit.topology.queueType == QueueType.QUORUM
                && rabbit.consumer.maxConcurrency >= rabbit.consumer.minConcurrency
                && rabbit.execution.heartbeatSeconds * 2L <= rabbit.execution.leaseSeconds
                && rabbit.outbox.claimLeaseSeconds * 1000L > rabbit.publisher.confirmTimeoutMillis
                && rabbit.outbox.maximumBackoffSeconds >= rabbit.outbox.initialBackoffSeconds
                && rabbit.retry.ttlSeconds != null
                && !rabbit.retry.ttlSeconds.isEmpty()
                && rabbit.retry.ttlSeconds.stream().allMatch(value -> value != null
                && value > 0
                && value <= Integer.MAX_VALUE / 1000L);
    }

    @Data
    public static class Rabbit {
        @Valid
        private Connection connection = new Connection();
        @Valid
        private Topology topology = new Topology();
        @Valid
        private Publisher publisher = new Publisher();
        @Valid
        private Outbox outbox = new Outbox();
        @Valid
        private Consumer consumer = new Consumer();
        @Valid
        private Execution execution = new Execution();
        @Valid
        private Retry retry = new Retry();
    }

    @Data
    public static class Connection {
        private String host;
        @Min(1)
        @Max(65535)
        private int port = 5672;
        private String virtualHost;
        private String username;
        private String password;

        boolean hasRequiredValues() {
            return hasText(host) && hasText(virtualHost) && hasText(username) && hasText(password);
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }

    @Data
    public static class Topology {
        @NotBlank
        private String mainExchange = "pmai.analysis.v1";
        @NotBlank
        private String mainQueue = "pmai.analysis.execute.v1";
        @NotBlank
        private String mainRoutingKey = "analysis.execute.v1";
        @NotBlank
        private String retryExchange = "pmai.analysis.retry.v1";
        @NotBlank
        private String deadLetterExchange = "pmai.analysis.dlx.v1";
        @NotBlank
        private String deadLetterQueue = "pmai.analysis.dead.v1";
        @NotBlank
        private String deadLetterRoutingKey = "analysis.dead.v1";
        private QueueType queueType = QueueType.QUORUM;
    }

    @Data
    public static class Publisher {
        @Min(100)
        private long confirmTimeoutMillis = 5000;
    }

    @Data
    public static class Outbox {
        @Min(100)
        private long pollIntervalMillis = 1000;
        @Min(1)
        @Max(500)
        private int batchSize = 50;
        @Min(1)
        private int claimLeaseSeconds = 30;
        @Min(1)
        @Max(20)
        private int maximumAttempts = 8;
        @Min(1)
        private int initialBackoffSeconds = 5;
        @Min(1)
        private int maximumBackoffSeconds = 300;
        @Min(1)
        private int publishedRetentionHours = 168;
    }

    @Data
    public static class Consumer {
        @Min(1)
        private int minConcurrency = 1;
        @Min(1)
        private int maxConcurrency = 4;
        @Min(1)
        private int prefetch = 1;
        @Min(1)
        @Max(1000)
        private int deliveryLimit = 20;
    }

    @Data
    public static class Execution {
        @Min(5)
        private int leaseSeconds = 120;
        @Min(1)
        private int heartbeatSeconds = 30;
        @Min(100)
        private long recoveryScanIntervalMillis = 5000;
        @Min(1)
        @Max(500)
        private int recoveryBatchSize = 50;
        @Min(1)
        @Max(10)
        private int maximumAttempts = 4;
    }

    @Data
    public static class Retry {
        private List<Long> ttlSeconds = new ArrayList<>(List.of(10L, 60L, 300L));
    }
}

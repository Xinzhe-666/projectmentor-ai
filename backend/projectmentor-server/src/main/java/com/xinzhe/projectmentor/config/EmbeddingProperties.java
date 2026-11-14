package com.xinzhe.projectmentor.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "projectmentor.embedding")
public class EmbeddingProperties {
    private boolean enabled;
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey;
    private String model = "text-embedding-3-small";
    @Min(1) @Max(65536)
    private int dimension = 1536;
    @Min(1) @Max(256)
    private int batchSize = 32;
    @Min(1) @Max(300)
    private int timeoutSeconds = 30;
    @Min(0) @Max(8)
    private int maxRetries = 3;
    @Min(128) @Max(100000)
    private int maxInputChars = 16000;
}

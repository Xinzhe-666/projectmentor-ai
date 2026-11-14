package com.xinzhe.projectmentor.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "projectmentor.retrieval")
public class RetrievalProperties {
    public enum Mode { LEXICAL, SHADOW, HYBRID }
    private Mode mode = Mode.LEXICAL;
    @Min(1) @Max(200)
    private int candidateLimit = 40;
    @Min(1) @Max(50)
    private int resultLimit = 8;
    @Min(1) @Max(16383)
    private int rrfK = 60;
    @Min(100) @Max(30000)
    private int timeoutMillis = 3000;
}

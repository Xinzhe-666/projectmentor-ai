package com.xinzhe.projectmentor.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "projectmentor.milvus")
public class MilvusProperties {
    private boolean enabled;
    private String uri = "http://localhost:19530";
    private String token;
    private String database = "pmai";
    private String collection = "pmai_code_chunks_v1";
    @Min(1) @Max(300)
    private int timeoutSeconds = 10;
    @Min(8) @Max(1024)
    private int maxFilePathLength = 768;
    @Min(4) @Max(64)
    private int hnswM = 16;
    @Min(16) @Max(1024)
    private int hnswEfConstruction = 128;
}

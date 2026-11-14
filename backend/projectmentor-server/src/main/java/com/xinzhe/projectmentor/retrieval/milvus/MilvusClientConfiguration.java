package com.xinzhe.projectmentor.retrieval.milvus;

import com.xinzhe.projectmentor.config.MilvusProperties;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "projectmentor.milvus", name = "enabled", havingValue = "true")
public class MilvusClientConfiguration {
    @Bean(destroyMethod = "close")
    MilvusClientV2 milvusClient(MilvusProperties properties) {
        if (properties.getToken() == null || properties.getToken().isBlank()) {
            throw new IllegalStateException("MILVUS_TOKEN is required when Milvus is enabled");
        }
        ConnectConfig config = ConnectConfig.builder()
                .uri(properties.getUri())
                .token(properties.getToken())
                .dbName(properties.getDatabase())
                .connectTimeoutMs(properties.getTimeoutSeconds() * 1000L)
                .rpcDeadlineMs(properties.getTimeoutSeconds() * 1000L)
                .enablePrecheck(true)
                .build();
        return new MilvusClientV2(config);
    }
}

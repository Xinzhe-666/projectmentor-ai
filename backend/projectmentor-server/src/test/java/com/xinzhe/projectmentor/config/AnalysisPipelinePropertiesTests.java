package com.xinzhe.projectmentor.config;

import com.xinzhe.projectmentor.analysis.service.LocalAnalysisTaskDispatcher;
import com.xinzhe.projectmentor.analysis.service.RabbitAnalysisTaskDispatcher;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisPipelinePropertiesTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void localModeIsDefaultAndDoesNotRequireRabbitCredentials() {
        AnalysisPipelineProperties properties = new AnalysisPipelineProperties();

        assertThat(properties.getDispatchMode()).isEqualTo(AnalysisPipelineProperties.DispatchMode.LOCAL);
        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void rabbitModeFailsWithoutCredentialsOrWithUnsafeLeaseSettings() {
        AnalysisPipelineProperties properties = validRabbitProperties();
        properties.getRabbit().getConnection().setPassword(" ");

        assertThat(validator.validate(properties)).isNotEmpty();

        properties = validRabbitProperties();
        properties.getRabbit().getExecution().setHeartbeatSeconds(61);
        properties.getRabbit().getExecution().setLeaseSeconds(120);

        assertThat(validator.validate(properties)).isNotEmpty();

        properties = validRabbitProperties();
        properties.getRabbit().getTopology().setQueueType(AnalysisPipelineProperties.QueueType.CLASSIC);

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void dispatchersAreMutuallyExclusiveByConfiguration() {
        ConditionalOnProperty local = LocalAnalysisTaskDispatcher.class.getAnnotation(ConditionalOnProperty.class);
        ConditionalOnProperty rabbit = RabbitAnalysisTaskDispatcher.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(local.havingValue()).isEqualTo("local");
        assertThat(local.matchIfMissing()).isTrue();
        assertThat(rabbit.havingValue()).isEqualTo("rabbit");
        assertThat(rabbit.matchIfMissing()).isFalse();
    }

    private AnalysisPipelineProperties validRabbitProperties() {
        AnalysisPipelineProperties properties = new AnalysisPipelineProperties();
        properties.setDispatchMode(AnalysisPipelineProperties.DispatchMode.RABBIT);
        properties.getRabbit().getConnection().setHost("rabbit");
        properties.getRabbit().getConnection().setVirtualHost("/pmai-test");
        properties.getRabbit().getConnection().setUsername("pmai-test");
        properties.getRabbit().getConnection().setPassword("test-only-password");
        return properties;
    }
}

package com.xinzhe.projectmentor.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RabbitAnalysisConfigTests {

    @Test
    void mainAndRetryQueuesUseAtLeastOnceDeadLetteringAndFiniteDeliveryLimit() {
        AnalysisPipelineProperties properties = new AnalysisPipelineProperties();
        properties.getRabbit().getConsumer().setDeliveryLimit(17);
        properties.getRabbit().getRetry().setTtlSeconds(java.util.List.of(10L, 60L));

        Declarables topology = new RabbitAnalysisConfig().analysisRabbitTopology(properties);
        Map<String, Queue> queues = topology.getDeclarablesByType(Queue.class).stream()
                .collect(Collectors.toMap(Queue::getName, Function.identity()));

        assertReliableDeadLetterQueue(
                queues.get("pmai.analysis.execute.v1"),
                "pmai.analysis.dlx.v1",
                "analysis.dead.v1",
                17
        );
        assertReliableDeadLetterQueue(
                queues.get("pmai.analysis.execute.v1.retry.10s"),
                "pmai.analysis.v1",
                "analysis.execute.v1",
                17
        );
        assertReliableDeadLetterQueue(
                queues.get("pmai.analysis.execute.v1.retry.60s"),
                "pmai.analysis.v1",
                "analysis.execute.v1",
                17
        );

        Queue deadQueue = queues.get("pmai.analysis.dead.v1");
        assertThat(deadQueue.getArguments()).containsEntry("x-queue-type", "quorum");
        assertThat(deadQueue.getArguments()).doesNotContainKeys(
                "x-dead-letter-exchange", "x-dead-letter-routing-key", "x-dead-letter-strategy"
        );
    }

    @Test
    void classicTopologyFailsFastEvenIfValidationIsBypassed() {
        AnalysisPipelineProperties properties = new AnalysisPipelineProperties();
        properties.getRabbit().getTopology().setQueueType(AnalysisPipelineProperties.QueueType.CLASSIC);

        assertThatThrownBy(() -> new RabbitAnalysisConfig().analysisRabbitTopology(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires quorum");
    }

    private void assertReliableDeadLetterQueue(Queue queue,
                                               String deadLetterExchange,
                                               String deadLetterRoutingKey,
                                               int deliveryLimit) {
        assertThat(queue).isNotNull();
        assertThat(queue.getArguments())
                .containsEntry("x-queue-type", "quorum")
                .containsEntry("x-dead-letter-strategy", "at-least-once")
                .containsEntry("x-overflow", "reject-publish")
                .containsEntry("x-delivery-limit", deliveryLimit)
                .containsEntry("x-dead-letter-exchange", deadLetterExchange)
                .containsEntry("x-dead-letter-routing-key", deadLetterRoutingKey);
    }
}

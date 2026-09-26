package com.xinzhe.projectmentor.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.xinzhe.projectmentor.analysis.message.AnalysisTaskMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitAnalysisTaskConsumerTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void successfulDurableDecisionIsManuallyAcked() throws Exception {
        AnalysisDeliveryCoordinator coordinator = mock(AnalysisDeliveryCoordinator.class);
        Channel channel = mock(Channel.class);
        AnalysisTaskMessage taskMessage = message();
        when(coordinator.handle(taskMessage, true)).thenReturn(AnalysisDeliveryCoordinator.DeliveryResult.ACK);

        new RabbitAnalysisTaskConsumer(objectMapper, coordinator).consume(raw(taskMessage, 11L), channel);

        verify(channel).basicAck(11L, false);
        verify(channel, never()).basicReject(11L, true);
    }

    @Test
    void explicitDeadLetterDecisionRejectsWithoutRequeue() throws Exception {
        AnalysisDeliveryCoordinator coordinator = mock(AnalysisDeliveryCoordinator.class);
        Channel channel = mock(Channel.class);
        AnalysisTaskMessage taskMessage = message();
        when(coordinator.handle(taskMessage, true)).thenReturn(AnalysisDeliveryCoordinator.DeliveryResult.DEAD_LETTER);

        new RabbitAnalysisTaskConsumer(objectMapper, coordinator).consume(raw(taskMessage, 12L), channel);

        verify(channel).basicReject(12L, false);
        verify(channel, never()).basicAck(12L, false);
    }

    @Test
    void databaseFailureIsNotAckedAndIsRequeued() throws Exception {
        AnalysisDeliveryCoordinator coordinator = mock(AnalysisDeliveryCoordinator.class);
        Channel channel = mock(Channel.class);
        AnalysisTaskMessage taskMessage = message();
        when(coordinator.handle(taskMessage, true)).thenThrow(new IllegalStateException("database unavailable"));

        new RabbitAnalysisTaskConsumer(objectMapper, coordinator).consume(raw(taskMessage, 13L), channel);

        verify(channel).basicReject(13L, true);
        verify(channel, never()).basicAck(13L, false);
    }

    private Message raw(AnalysisTaskMessage taskMessage, long deliveryTag) throws Exception {
        return MessageBuilder.withBody(objectMapper.writeValueAsBytes(taskMessage))
                .setDeliveryTag(deliveryTag)
                .setMessageId(taskMessage.messageId())
                .build();
    }

    private AnalysisTaskMessage message() {
        return new AnalysisTaskMessage(
                "message-1", 1, 100L, 42L, 7L, 1, "correlation-1", Instant.now()
        );
    }
}

package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisOutboxEvent;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class AnalysisRabbitPublisherTests {

    @Test
    void confirmAckWithoutReturnIsReliableAndMessageIsPersistent() {
        Fixture fixture = fixture();
        doAnswer(invocation -> {
            Message message = invocation.getArgument(2);
            CorrelationData correlation = invocation.getArgument(3);
            assertThat(message.getMessageProperties().getDeliveryMode().name()).isEqualTo("PERSISTENT");
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(fixture.rabbitTemplate).send(eq("exchange"), eq("route"), any(Message.class), any(CorrelationData.class));

        assertThat(fixture.publisher.publish(event()).status()).isEqualTo(RabbitPublishResult.Status.ACK);
    }

    @Test
    void nackAndReturnAreNeverReportedAsPublished() {
        Fixture nack = fixture();
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "broker nack"));
            return null;
        }).when(nack.rabbitTemplate).send(any(), any(), any(Message.class), any(CorrelationData.class));
        assertThat(nack.publisher.publish(event()).status()).isEqualTo(RabbitPublishResult.Status.NACK);

        Fixture returned = fixture();
        doAnswer(invocation -> {
            Message message = invocation.getArgument(2);
            CorrelationData correlation = invocation.getArgument(3);
            correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE", "exchange", "route"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(returned.rabbitTemplate).send(any(), any(), any(Message.class), any(CorrelationData.class));
        assertThat(returned.publisher.publish(event()).status()).isEqualTo(RabbitPublishResult.Status.RETURNED);
    }

    @Test
    void missingConfirmAndConnectionFailureAreNotReliable() {
        Fixture timeout = fixture();
        timeout.properties.getRabbit().getPublisher().setConfirmTimeoutMillis(100);
        assertThat(timeout.publisher.publish(event()).status()).isEqualTo(RabbitPublishResult.Status.TIMEOUT);

        Fixture error = fixture();
        doThrow(new IllegalStateException("amqp://user:secret@broker connection refused"))
                .when(error.rabbitTemplate).send(any(), any(), any(Message.class), any(CorrelationData.class));
        RabbitPublishResult result = error.publisher.publish(event());
        assertThat(result.status()).isEqualTo(RabbitPublishResult.Status.ERROR);
        assertThat(result.reason()).doesNotContain("secret");
    }

    private Fixture fixture() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        AnalysisPipelineProperties properties = new AnalysisPipelineProperties();
        return new Fixture(template, properties, new AnalysisRabbitPublisher(template, properties));
    }

    private AnalysisOutboxEvent event() {
        AnalysisOutboxEvent event = new AnalysisOutboxEvent();
        event.setEventId("message-1");
        event.setExchangeName("exchange");
        event.setRoutingKey("route");
        event.setSchemaVersion(1);
        event.setPayload("{\"taskId\":1}");
        return event;
    }

    private record Fixture(RabbitTemplate rabbitTemplate,
                           AnalysisPipelineProperties properties,
                           AnalysisRabbitPublisher publisher) {
    }
}

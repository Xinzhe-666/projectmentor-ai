package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisOutboxEvent;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "projectmentor.analysis", name = "dispatch-mode", havingValue = "rabbit")
public class AnalysisRabbitPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final AnalysisPipelineProperties properties;

    public RabbitPublishResult publish(AnalysisOutboxEvent event) {
        CorrelationData correlationData = new CorrelationData(event.getEventId());
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        messageProperties.setMessageId(event.getEventId());
        messageProperties.setCorrelationId(event.getEventId());
        messageProperties.setHeader("pmai-schema-version", event.getSchemaVersion());
        Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), messageProperties);

        try {
            rabbitTemplate.send(event.getExchangeName(), event.getRoutingKey(), message, correlationData);
            CorrelationData.Confirm confirm = correlationData.getFuture().get(
                    properties.getRabbit().getPublisher().getConfirmTimeoutMillis(), TimeUnit.MILLISECONDS
            );
            if (!confirm.isAck()) {
                return new RabbitPublishResult(
                        RabbitPublishResult.Status.NACK,
                        SafePipelineError.sanitize(confirm.getReason())
                );
            }
            if (correlationData.getReturned() != null) {
                return new RabbitPublishResult(
                        RabbitPublishResult.Status.RETURNED,
                        SafePipelineError.sanitize(correlationData.getReturned().getReplyText())
                );
            }
            return new RabbitPublishResult(RabbitPublishResult.Status.ACK, null);
        } catch (TimeoutException e) {
            return new RabbitPublishResult(RabbitPublishResult.Status.TIMEOUT, "publisher confirm timeout");
        } catch (Exception e) {
            return new RabbitPublishResult(RabbitPublishResult.Status.ERROR, SafePipelineError.from(e));
        }
    }
}

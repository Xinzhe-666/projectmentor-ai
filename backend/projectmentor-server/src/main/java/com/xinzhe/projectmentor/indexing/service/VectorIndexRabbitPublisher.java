package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.analysis.service.RabbitPublishResult;
import com.xinzhe.projectmentor.analysis.service.SafePipelineError;
import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexOutboxEvent;
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
@ConditionalOnProperty(prefix = "projectmentor.vector-index", name = "dispatch-mode", havingValue = "rabbit")
public class VectorIndexRabbitPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final VectorIndexProperties properties;

    public RabbitPublishResult publish(VectorIndexOutboxEvent event) {
        CorrelationData correlation = new CorrelationData(event.getEventId());
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding(StandardCharsets.UTF_8.name());
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setMessageId(event.getEventId());
        props.setCorrelationId(event.getEventId());
        props.setHeader("pmai-schema-version", event.getSchemaVersion());
        try {
            rabbitTemplate.send(event.getExchangeName(), event.getRoutingKey(),
                    new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), props), correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(
                    properties.getPublisherConfirmTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) return new RabbitPublishResult(RabbitPublishResult.Status.NACK,
                    SafePipelineError.sanitize(confirm.getReason()));
            if (correlation.getReturned() != null) return new RabbitPublishResult(RabbitPublishResult.Status.RETURNED,
                    SafePipelineError.sanitize(correlation.getReturned().getReplyText()));
            return new RabbitPublishResult(RabbitPublishResult.Status.ACK, null);
        } catch (TimeoutException e) {
            return new RabbitPublishResult(RabbitPublishResult.Status.TIMEOUT, "publisher confirm timeout");
        } catch (Exception e) {
            return new RabbitPublishResult(RabbitPublishResult.Status.ERROR, SafePipelineError.from(e));
        }
    }
}

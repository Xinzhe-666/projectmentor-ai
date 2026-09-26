package com.xinzhe.projectmentor.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.xinzhe.projectmentor.analysis.message.AnalysisTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "projectmentor.analysis", name = "dispatch-mode", havingValue = "rabbit")
public class RabbitAnalysisTaskConsumer {

    private final ObjectMapper objectMapper;
    private final AnalysisDeliveryCoordinator coordinator;

    @RabbitListener(
            queues = "${projectmentor.analysis.rabbit.topology.main-queue:pmai.analysis.execute.v1}",
            containerFactory = "analysisRabbitListenerContainerFactory"
    )
    public void consume(Message rawMessage, Channel channel) throws Exception {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        AnalysisTaskMessage message;
        try {
            message = objectMapper.readValue(rawMessage.getBody(), AnalysisTaskMessage.class);
        } catch (Exception e) {
            log.warn("analysis_message_invalid messageId={} reason={}",
                    rawMessage.getMessageProperties().getMessageId(), SafePipelineError.from(e));
            channel.basicReject(deliveryTag, false);
            return;
        }

        try {
            AnalysisDeliveryCoordinator.DeliveryResult result = coordinator.handle(message, true);
            if (result == AnalysisDeliveryCoordinator.DeliveryResult.ACK) {
                channel.basicAck(deliveryTag, false);
            } else {
                channel.basicReject(deliveryTag, false);
            }
        } catch (Exception e) {
            log.error(
                    "analysis_consume_persistence_failed taskId={} messageId={} correlationId={} reason={}",
                    message.taskId(), message.messageId(), message.correlationId(), SafePipelineError.from(e)
            );
            // Quorum delivery-count is incremented by AMQP 0-9-1 basic.reject, not basic.nack.
            // Requeue remains true so transient database failures are retried, but x-delivery-limit
            // bounds the loop and ultimately dead-letters the message.
            channel.basicReject(deliveryTag, true);
        }
    }
}

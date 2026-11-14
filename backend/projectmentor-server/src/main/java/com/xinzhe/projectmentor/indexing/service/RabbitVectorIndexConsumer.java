package com.xinzhe.projectmentor.indexing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.xinzhe.projectmentor.analysis.service.SafePipelineError;
import com.xinzhe.projectmentor.indexing.message.VectorIndexMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "projectmentor.vector-index", name = "dispatch-mode", havingValue = "rabbit")
@ConditionalOnBean(VectorIndexWorker.class)
public class RabbitVectorIndexConsumer {
    private final ObjectMapper objectMapper;
    private final VectorIndexWorker worker;
    private final VectorIndexRetryService retries;

    @RabbitListener(queues = "${projectmentor.vector-index.main-queue:pmai.index.execute.v1}",
            containerFactory = "vectorIndexRabbitListenerContainerFactory")
    public void consume(Message raw, Channel channel) throws Exception {
        long tag = raw.getMessageProperties().getDeliveryTag();
        VectorIndexMessage message;
        try {
            message = objectMapper.readValue(raw.getBody(), VectorIndexMessage.class);
        } catch (Exception invalid) {
            channel.basicReject(tag, false);
            return;
        }
        try {
            VectorIndexWorker.ProcessingResult result = worker.process(message);
            if (result == VectorIndexWorker.ProcessingResult.ACK) channel.basicAck(tag, false);
            else if (result == VectorIndexWorker.ProcessingResult.RETRY_LATER && retries.schedule(message)) {
                channel.basicAck(tag, false);
            } else channel.basicReject(tag, false);
        } catch (Exception persistenceFailure) {
            log.error("vector_index_consume_failed taskId={} reason={}", message.taskId(),
                    SafePipelineError.from(persistenceFailure));
            channel.basicReject(tag, true);
        }
    }
}

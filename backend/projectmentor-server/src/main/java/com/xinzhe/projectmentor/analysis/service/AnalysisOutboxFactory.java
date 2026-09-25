package com.xinzhe.projectmentor.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisOutboxEvent;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.message.AnalysisTaskMessage;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import com.xinzhe.projectmentor.config.RabbitAnalysisConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AnalysisOutboxFactory {

    public static final String EXECUTE_EVENT = "ANALYSIS_EXECUTE";
    public static final String DEAD_EVENT = "ANALYSIS_DEAD";

    private final ObjectMapper objectMapper;
    private final AnalysisPipelineProperties properties;

    public AnalysisOutboxEvent initial(AnalysisTask task) {
        AnalysisPipelineProperties.Topology topology = properties.getRabbit().getTopology();
        return create(task, 1, EXECUTE_EVENT, topology.getMainExchange(), topology.getMainRoutingKey());
    }

    public AnalysisOutboxEvent retry(AnalysisTask task, int nextAttempt, long ttlSeconds) {
        AnalysisPipelineProperties.Topology topology = properties.getRabbit().getTopology();
        return create(
                task,
                nextAttempt,
                EXECUTE_EVENT,
                topology.getRetryExchange(),
                RabbitAnalysisConfig.retryRoutingKey(ttlSeconds)
        );
    }

    public AnalysisOutboxEvent dead(AnalysisTask task) {
        AnalysisPipelineProperties.Topology topology = properties.getRabbit().getTopology();
        return create(
                task,
                Math.max(1, task.getExecutionAttempt()),
                DEAD_EVENT,
                topology.getDeadLetterExchange(),
                topology.getDeadLetterRoutingKey()
        );
    }

    private AnalysisOutboxEvent create(AnalysisTask task,
                                       int attempt,
                                       String eventType,
                                       String exchange,
                                       String routingKey) {
        String messageId = UUID.randomUUID().toString();
        AnalysisTaskMessage message = new AnalysisTaskMessage(
                messageId,
                AnalysisTaskMessage.CURRENT_SCHEMA_VERSION,
                task.getId(),
                task.getProjectId(),
                task.getUserId(),
                attempt,
                task.getCorrelationId(),
                Instant.now()
        );

        AnalysisOutboxEvent event = new AnalysisOutboxEvent();
        event.setEventId(messageId);
        event.setTaskId(task.getId());
        event.setEventType(eventType);
        event.setSchemaVersion(AnalysisTaskMessage.CURRENT_SCHEMA_VERSION);
        event.setPayload(toJson(message));
        event.setExchangeName(exchange);
        event.setRoutingKey(routingKey);
        event.setStatus("NEW");
        event.setPublishAttempt(0);
        event.setNextAttemptAt(LocalDateTime.now());
        return event;
    }

    private String toJson(AnalysisTaskMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分析任务消息创建失败");
        }
    }
}

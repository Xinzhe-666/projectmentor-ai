package com.xinzhe.projectmentor.indexing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.config.RabbitVectorIndexConfig;
import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexOutboxEvent;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexTask;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexOutboxMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import com.xinzhe.projectmentor.indexing.message.VectorIndexMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VectorIndexRetryService {
    private final VectorIndexTaskMapper taskMapper;
    private final VectorIndexOutboxMapper outboxMapper;
    private final VectorIndexProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public boolean schedule(VectorIndexMessage prior) {
        VectorIndexTask task = taskMapper.selectById(prior.taskId());
        if (task == null || !"PENDING".equals(task.getStatus()) || task.getExecutionAttempt() >= properties.getMaximumAttempts()) {
            return false;
        }
        long ttl = retryTtl(task.getExecutionAttempt());
        VectorIndexMessage next = new VectorIndexMessage(UUID.randomUUID().toString(), 1, "INDEX_RETRY",
                task.getId(), task.getUserId(), task.getProjectId(), task.getGeneration(), task.getCorrelationId());
        VectorIndexOutboxEvent event = new VectorIndexOutboxEvent();
        event.setEventId(next.messageId()); event.setTaskId(task.getId()); event.setEventType(next.eventType());
        event.setSchemaVersion(1); event.setPayload(json(next)); event.setExchangeName(properties.getRetryExchange());
        event.setRoutingKey(RabbitVectorIndexConfig.retryRoutingKey(ttl)); event.setStatus("NEW");
        event.setPublishAttempt(0); event.setNextAttemptAt(LocalDateTime.now());
        return outboxMapper.insert(event) == 1;
    }

    private long retryTtl(int completedAttempt) {
        List<Long> ttl = properties.getRetryTtlSeconds();
        return ttl.get(Math.min(Math.max(0, completedAttempt - 1), ttl.size() - 1));
    }
    private String json(VectorIndexMessage message) {
        try { return objectMapper.writeValueAsString(message); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
}

package com.xinzhe.projectmentor.indexing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.config.EmbeddingProperties;
import com.xinzhe.projectmentor.config.MilvusProperties;
import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.indexing.entity.ProjectVectorIndex;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexOutboxEvent;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexTask;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexOutboxMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import com.xinzhe.projectmentor.indexing.message.VectorIndexMessage;
import com.xinzhe.projectmentor.indexing.vo.VectorIndexStatusVO;
import com.xinzhe.projectmentor.project.entity.Project;
import com.xinzhe.projectmentor.project.mapper.ProjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VectorIndexTaskSubmissionService {
    private final ProjectMapper projectMapper;
    private final ProjectVectorIndexMapper indexMapper;
    private final VectorIndexTaskMapper taskMapper;
    private final VectorIndexOutboxMapper outboxMapper;
    private final EmbeddingProperties embedding;
    private final MilvusProperties milvus;
    private final VectorIndexProperties properties;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry metrics;

    @Transactional(rollbackFor = Exception.class)
    public VectorIndexStatusVO rebuild(long projectId) {
        long userId = currentUserId();
        requireOwnedProject(projectId, userId);
        if (!embedding.isEnabled() || !milvus.isEnabled()) {
            throw new BusinessException(ErrorCode.VECTOR_INDEX_UNAVAILABLE,
                    "向量索引尚未配置，请继续使用基础检索");
        }
        return schedule(projectId, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void scheduleIfEnabled(long projectId) {
        if (!embedding.isEnabled() || !milvus.isEnabled()) return;
        long userId = currentUserId();
        requireOwnedProject(projectId, userId);
        schedule(projectId, userId);
    }

    private VectorIndexStatusVO schedule(long projectId, long userId) {
        String activeKey = "project:" + projectId;
        VectorIndexTask existing = taskMapper.selectActive(activeKey);
        if (existing != null) return toStatus(projectId, indexMapper.selectOwned(projectId, userId), existing);

        indexMapper.ensure(projectId, userId, milvus.getCollection());
        ProjectVectorIndex state = indexMapper.selectForUpdate(projectId);
        existing = taskMapper.selectActive(activeKey);
        if (existing != null) return toStatus(projectId, state, existing);

        long generation = Math.max(value(state.getActiveGeneration()), value(state.getPendingGeneration())) + 1;
        String correlationId = UUID.randomUUID().toString();
        VectorIndexTask task = new VectorIndexTask();
        task.setProjectId(projectId); task.setUserId(userId); task.setTaskType("REBUILD");
        task.setActiveKey(activeKey); task.setCorrelationId(correlationId); task.setGeneration(generation);
        task.setStatus("PENDING"); task.setProgress(0); task.setExecutionAttempt(0); task.setExecutionVersion(0L);
        if (taskMapper.insert(task) != 1) throw new IllegalStateException("Failed to create vector index task");
        if (indexMapper.markPending(projectId, userId, generation, embedding.getModel(), embedding.getDimension()) != 1) {
            throw new IllegalStateException("Failed to reserve vector index generation");
        }

        VectorIndexMessage message = new VectorIndexMessage(UUID.randomUUID().toString(), 1, "INDEX_EXECUTE",
                task.getId(), userId, projectId, generation, correlationId);
        VectorIndexOutboxEvent outbox = outbox(task.getId(), message);
        if (outboxMapper.insert(outbox) != 1) throw new IllegalStateException("Failed to create vector index outbox");
        metrics.counter("pmai.index.task", "result", "created").increment();
        eventPublisher.publishEvent(new VectorIndexTaskCreatedEvent(message, outbox.getId()));
        return toStatus(projectId, indexMapper.selectOwned(projectId, userId), task);
    }

    public VectorIndexStatusVO status(long projectId) {
        long userId = currentUserId();
        requireOwnedProject(projectId, userId);
        ProjectVectorIndex state = indexMapper.selectOwned(projectId, userId);
        return toStatus(projectId, state, taskMapper.selectActive("project:" + projectId));
    }

    private VectorIndexOutboxEvent outbox(long taskId, VectorIndexMessage message) {
        VectorIndexOutboxEvent event = new VectorIndexOutboxEvent();
        event.setEventId(message.messageId()); event.setTaskId(taskId); event.setEventType(message.eventType());
        event.setSchemaVersion(1); event.setPayload(json(message)); event.setExchangeName(properties.getMainExchange());
        event.setRoutingKey(properties.getMainRoutingKey()); event.setStatus("NEW"); event.setPublishAttempt(0);
        event.setNextAttemptAt(LocalDateTime.now());
        return event;
    }

    private String json(VectorIndexMessage message) {
        try { return objectMapper.writeValueAsString(message); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Failed to serialize vector index message", e); }
    }

    private void requireOwnedProject(long projectId, long userId) {
        Project project = projectMapper.selectOne(new LambdaQueryWrapper<Project>().eq(Project::getId, projectId)
                .eq(Project::getUserId, userId).last("LIMIT 1"));
        if (project == null) throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权访问");
    }

    private long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return userId;
    }

    private VectorIndexStatusVO toStatus(long projectId, ProjectVectorIndex state, VectorIndexTask task) {
        String status = state == null ? "NOT_INDEXED" : state.getStatus();
        return VectorIndexStatusVO.builder().projectId(projectId)
                .status(status).displayStatus(display(status)).activeGeneration(state == null ? null : state.getActiveGeneration())
                .chunkCount(state == null ? 0 : state.getChunkCount()).indexedFileCount(state == null ? 0 : state.getIndexedFileCount())
                .embeddingModel(state == null ? null : state.getEmbeddingModel())
                .lastErrorCode(state == null ? null : state.getLastErrorCode())
                .lastErrorMessage(state == null ? null : state.getLastErrorMessage())
                .lastIndexedAt(state == null ? null : state.getLastIndexedAt())
                .activeTaskId(task == null ? null : task.getId()).progress(task == null ? null : task.getProgress()).build();
    }

    private String display(String status) {
        return Map.of("NOT_INDEXED", "未建立", "PENDING", "正在索引", "RUNNING", "正在索引", "READY", "可用",
                "DEGRADED", "内容已变化", "FAILED", "失败", "DELETING", "正在删除").getOrDefault(status, status);
    }
    private long value(Long value) { return value == null ? 0 : value; }

    public record VectorIndexTaskCreatedEvent(VectorIndexMessage message, long outboxId) {}
}

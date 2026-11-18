package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexTask;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import com.xinzhe.projectmentor.indexing.message.VectorIndexMessage;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "projectmentor.vector-index", name = "dispatch-mode", havingValue = "rabbit")
@ConditionalOnBean(VectorIndexWorker.class)
public class VectorIndexLeaseRecoveryScanner {
    private final VectorIndexTaskMapper taskMapper;
    private final ProjectVectorIndexMapper indexMapper;
    private final VectorIndexRetryService retries;
    private final VectorIndexProperties properties;
    private final MeterRegistry metrics;

    @Scheduled(fixedDelayString = "${projectmentor.vector-index.recovery-scan-interval-millis:5000}")
    @Transactional(rollbackFor = Exception.class)
    public void recover() {
        for (VectorIndexTask task : taskMapper.selectExpiredForUpdate(50)) {
            if (task.getExecutionAttempt() >= properties.getMaximumAttempts()) {
                if (taskMapper.failTerminal(task.getId(), "INDEX_LEASE_EXHAUSTED", "执行租约过期且已达到最大尝试次数") == 1) {
                    if (!"CLEANUP".equals(task.getTaskType())) {
                        indexMapper.failPending(task.getProjectId(), task.getUserId(), task.getGeneration(), task.getBuildId(),
                                "INDEX_LEASE_EXHAUSTED", "执行租约过期且已达到最大尝试次数");
                    }
                    metrics.counter("pmai.index.task", "result", "recovery-failed").increment();
                }
            } else if (taskMapper.recoverExpired(task.getId(), task.getExecutionVersion(), "执行租约过期，任务已恢复") == 1) {
                String eventType = "CLEANUP".equals(task.getTaskType()) ? "PROJECT_DELETE" : "INDEX_RECOVERY";
                retries.schedule(new VectorIndexMessage(UUID.randomUUID().toString(), 1, eventType,
                        task.getId(), task.getUserId(), task.getProjectId(), task.getGeneration(), task.getCorrelationId()));
                metrics.counter("pmai.index.task", "result", "recovered").increment();
            }
        }
    }
}

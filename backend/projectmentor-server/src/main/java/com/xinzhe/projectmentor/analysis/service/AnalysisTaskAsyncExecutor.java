package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.analysis.message.AnalysisTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisTaskAsyncExecutor {

    private final AnalysisTaskMapper taskMapper;

    private final AnalysisDeliveryCoordinator coordinator;

    @Async("analysisTaskExecutor")
    public void executeAnalysisTask(Long taskId) {
        try {
            AnalysisTask task = taskMapper.selectById(taskId);
            if (task == null) {
                return;
            }
            AnalysisTaskMessage message = new AnalysisTaskMessage(
                    UUID.randomUUID().toString(),
                    AnalysisTaskMessage.CURRENT_SCHEMA_VERSION,
                    task.getId(),
                    task.getProjectId(),
                    task.getUserId(),
                    1,
                    task.getCorrelationId(),
                    Instant.now()
            );
            coordinator.handle(message, false);
        } catch (Exception e) {
            log.error("local_analysis_dispatch_failed taskId={} reason={}", taskId, SafePipelineError.from(e));
        }
    }
}

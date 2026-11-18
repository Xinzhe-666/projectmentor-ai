package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.indexing.entity.VectorIndexTask;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexOutboxMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VectorIndexTerminalTransitionService {
    private final VectorIndexOutboxMapper outboxMapper;
    private final VectorIndexTaskMapper taskMapper;
    private final ProjectVectorIndexMapper indexMapper;

    @Transactional(rollbackFor = Exception.class)
    public void failOutbox(long outboxId, String owner, long taskId, String reason) {
        if (outboxMapper.markFailed(outboxId, owner, reason) != 1) return;
        VectorIndexTask task = taskMapper.selectById(taskId);
        if (task == null || "SUCCEEDED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) return;
        if (taskMapper.failTerminal(taskId, "INDEX_OUTBOX_EXHAUSTED", "索引消息发布超过最大尝试次数") != 1) {
            throw new IllegalStateException("Failed to terminate index task after outbox exhaustion");
        }
        if (!"CLEANUP".equals(task.getTaskType())) {
            indexMapper.failPending(task.getProjectId(), task.getUserId(), task.getGeneration(), task.getBuildId(),
                    "INDEX_OUTBOX_EXHAUSTED", "索引消息发布超过最大尝试次数");
        }
    }
}

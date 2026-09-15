package com.xinzhe.projectmentor.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.analysis.vo.AnalysisTaskVO;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskProgressService {

    private static final String TASK_PROGRESS_KEY_PREFIX = "analysis:task:";

    private final AnalysisTaskMapper analysisTaskMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    public void updateProgress(Long taskId,
                               String status,
                               Integer progress,
                               String message,
                               Long reportId,
                               String failReason,
                               boolean finished) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);

        if (task == null) {
            log.warn("Analysis task not found, taskId={}", taskId);
            return;
        }

        task.setStatus(status);
        task.setProgress(progress);
        task.setReportId(reportId);
        task.setFailReason(failReason);

        boolean terminal = isTerminalStatus(status);
        if (terminal) {
            task.setActiveKey(null);
        }

        if (finished || terminal) {
            task.setFinishTime(LocalDateTime.now());
        }

        if (analysisTaskMapper.updateTaskProgress(task) != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务进度保存失败，请稍后重试");
        }

        AnalysisTaskVO vo = AnalysisTaskVO.builder()
                .taskId(task.getId())
                .projectId(task.getProjectId())
                .taskType(task.getTaskType())
                .status(status)
                .progress(progress)
                .reportId(reportId)
                .failReason(failReason)
                .message(message)
                .createTime(task.getCreateTime())
                .finishTime(task.getFinishTime())
                .build();

        saveProgressToRedis(taskId, vo);
    }

    public AnalysisTaskVO getProgress(Long taskId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);

        if (task == null) {
            return null;
        }

        String message = buildMessage(task.getStatus());

        try {
            String json = stringRedisTemplate.opsForValue().get(buildRedisKey(taskId));

            if (json != null && !json.isBlank()) {
                AnalysisTaskVO cached = objectMapper.readValue(json, AnalysisTaskVO.class);
                if (Objects.equals(cached.getStatus(), task.getStatus())
                        && Objects.equals(cached.getProgress(), task.getProgress())
                        && cached.getMessage() != null
                        && !cached.getMessage().isBlank()) {
                    message = cached.getMessage();
                }
            }
        } catch (Exception e) {
            log.warn("Read task progress from Redis failed, taskId={}", taskId, e);
        }

        return AnalysisTaskVO.builder()
                .taskId(task.getId())
                .projectId(task.getProjectId())
                .taskType(task.getTaskType())
                .status(task.getStatus())
                .progress(task.getProgress())
                .reportId(task.getReportId())
                .failReason(task.getFailReason())
                .message(message)
                .createTime(task.getCreateTime())
                .finishTime(task.getFinishTime())
                .build();
    }

    private void saveProgressToRedis(Long taskId, AnalysisTaskVO vo) {
        try {
            String json = objectMapper.writeValueAsString(vo);
            stringRedisTemplate.opsForValue().set(
                    buildRedisKey(taskId),
                    json,
                    Duration.ofHours(2)
            );
        } catch (JsonProcessingException e) {
            log.warn("Serialize task progress failed, taskId={}", taskId, e);
        } catch (Exception e) {
            // 【重点理解】
            // Redis 只是进度缓存，Redis 失败不能让整个分析任务失败。
            log.warn("Save task progress to Redis failed, taskId={}", taskId, e);
        }
    }

    private String buildRedisKey(Long taskId) {
        return TASK_PROGRESS_KEY_PREFIX + taskId;
    }

    private String buildMessage(String status) {
        if ("PENDING".equalsIgnoreCase(status)) {
            return "任务等待执行";
        }
        if ("RUNNING".equalsIgnoreCase(status)) {
            return "任务正在执行";
        }
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return "任务执行成功";
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            return "任务执行失败";
        }
        return "任务状态未知";
    }

    private boolean isTerminalStatus(String status) {
        return "SUCCESS".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status);
    }
}

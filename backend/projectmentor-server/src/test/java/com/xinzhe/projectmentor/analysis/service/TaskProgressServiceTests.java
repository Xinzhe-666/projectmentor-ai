package com.xinzhe.projectmentor.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.analysis.vo.AnalysisTaskVO;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskProgressServiceTests {

    private AnalysisTaskMapper analysisTaskMapper;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private TaskProgressService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        analysisTaskMapper = mock(AnalysisTaskMapper.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = mock(ObjectMapper.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        service = new TaskProgressService(analysisTaskMapper, redisTemplate, objectMapper);
    }

    @Test
    void terminalUpdateClearsActiveKeyAndSetsFinishTime() {
        AnalysisTask task = task("RUNNING", 35);
        when(analysisTaskMapper.selectById(10L)).thenReturn(task);
        when(analysisTaskMapper.updateTaskProgress(any(AnalysisTask.class))).thenReturn(1);

        service.updateProgress(10L, "SUCCESS", 100, "完成", 99L, null, true);

        assertThat(task.getStatus()).isEqualTo("SUCCESS");
        assertThat(task.getProgress()).isEqualTo(100);
        assertThat(task.getReportId()).isEqualTo(99L);
        assertThat(task.getActiveKey()).isNull();
        assertThat(task.getFinishTime()).isNotNull();
        verify(analysisTaskMapper).updateTaskProgress(task);
        verify(valueOperations).set(
                anyString(),
                anyString(),
                any(Duration.class)
        );
    }

    @Test
    void mapperUpdateExplicitlyPersistsNullableTerminalFields() throws Exception {
        Update update = AnalysisTaskMapper.class
                .getMethod("updateTaskProgress", AnalysisTask.class)
                .getAnnotation(Update.class);

        assertThat(update).isNotNull();
        String sql = String.join(" ", Arrays.asList(update.value()));
        assertThat(sql)
                .contains("finish_time = #{task.finishTime}")
                .contains("active_key = #{task.activeKey}");
    }

    @Test
    void mysqlRemainsSourceOfTruthWhenRedisContainsStaleProgress() throws Exception {
        AnalysisTask task = task("RUNNING", 35);
        task.setReportId(null);
        task.setFinishTime(null);
        when(analysisTaskMapper.selectById(10L)).thenReturn(task);
        when(valueOperations.get("analysis:task:10")).thenReturn("stale-json");
        when(objectMapper.readValue("stale-json", AnalysisTaskVO.class)).thenReturn(
                AnalysisTaskVO.builder()
                        .taskId(10L)
                        .projectId(42L)
                        .status("SUCCESS")
                        .progress(100)
                        .reportId(999L)
                        .message("过期缓存")
                        .finishTime(LocalDateTime.now())
                        .build()
        );

        AnalysisTaskVO result = service.getProgress(10L);

        verify(analysisTaskMapper).selectById(10L);
        assertThat(result.getStatus()).isEqualTo("RUNNING");
        assertThat(result.getProgress()).isEqualTo(35);
        assertThat(result.getReportId()).isNull();
        assertThat(result.getFinishTime()).isNull();
        assertThat(result.getMessage()).isEqualTo("任务正在执行");
    }

    @Test
    void redisFailureDoesNotPreventMysqlTerminalUpdate() throws Exception {
        AnalysisTask task = task("RUNNING", 35);
        when(analysisTaskMapper.selectById(10L)).thenReturn(task);
        when(analysisTaskMapper.updateTaskProgress(any(AnalysisTask.class))).thenReturn(1);
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("redis serialization failed"));

        service.updateProgress(10L, "FAILED", 100, "失败", null, "内部原因", true);

        assertThat(task.getStatus()).isEqualTo("FAILED");
        assertThat(task.getActiveKey()).isNull();
        assertThat(task.getFinishTime()).isNotNull();
        verify(analysisTaskMapper).updateTaskProgress(task);
    }

    private AnalysisTask task(String status, int progress) {
        AnalysisTask task = new AnalysisTask();
        task.setId(10L);
        task.setProjectId(42L);
        task.setTaskType("FULL_ANALYSIS");
        task.setActiveKey("FULL_ANALYSIS:42");
        task.setStatus(status);
        task.setProgress(progress);
        task.setCreateTime(LocalDateTime.now().minusMinutes(1));
        return task;
    }
}

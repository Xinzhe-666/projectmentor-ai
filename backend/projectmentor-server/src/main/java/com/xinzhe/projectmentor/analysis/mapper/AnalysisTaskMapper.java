package com.xinzhe.projectmentor.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface AnalysisTaskMapper extends BaseMapper<AnalysisTask> {

    @Update("""
            UPDATE pm_analysis_task
            SET status = #{task.status},
                progress = #{task.progress},
                report_id = #{task.reportId},
                fail_reason = #{task.failReason},
                finish_time = #{task.finishTime},
                active_key = #{task.activeKey}
            WHERE id = #{task.id}
            """)
    int updateTaskProgress(@Param("task") AnalysisTask task);

    @Update("""
            UPDATE pm_analysis_task
            SET status = 'RUNNING',
                progress = GREATEST(progress, 5),
                worker_id = #{workerId},
                lease_expires_at = TIMESTAMPADD(SECOND, #{leaseSeconds}, NOW(6)),
                heartbeat_at = NOW(6),
                execution_attempt = execution_attempt + 1,
                execution_version = execution_version + 1,
                last_message_id = #{messageId},
                retry_reason = NULL
            WHERE id = #{taskId}
              AND user_id = #{userId}
              AND project_id = #{projectId}
              AND correlation_id = #{correlationId}
              AND active_key IS NOT NULL
              AND execution_attempt < #{maximumAttempts}
              AND (status = 'PENDING'
                   OR (status = 'RUNNING' AND lease_expires_at <= NOW(6)))
            """)
    int claimExecution(@Param("taskId") Long taskId,
                       @Param("userId") Long userId,
                       @Param("projectId") Long projectId,
                       @Param("correlationId") String correlationId,
                       @Param("workerId") String workerId,
                       @Param("messageId") String messageId,
                       @Param("leaseSeconds") int leaseSeconds,
                       @Param("maximumAttempts") int maximumAttempts);

    @Update("""
            UPDATE pm_analysis_task
            SET heartbeat_at = NOW(6),
                lease_expires_at = TIMESTAMPADD(SECOND, #{leaseSeconds}, NOW(6))
            WHERE id = #{taskId}
              AND status = 'RUNNING'
              AND worker_id = #{workerId}
              AND execution_version = #{executionVersion}
              AND lease_expires_at > NOW(6)
            """)
    int renewLease(@Param("taskId") Long taskId,
                   @Param("workerId") String workerId,
                   @Param("executionVersion") Long executionVersion,
                   @Param("leaseSeconds") int leaseSeconds);

    @Update("""
            UPDATE pm_analysis_task
            SET progress = #{progress}
            WHERE id = #{taskId}
              AND status = 'RUNNING'
              AND worker_id = #{workerId}
              AND execution_version = #{executionVersion}
              AND lease_expires_at > NOW(6)
            """)
    int updateFencedProgress(@Param("taskId") Long taskId,
                             @Param("workerId") String workerId,
                             @Param("executionVersion") Long executionVersion,
                             @Param("progress") int progress);

    @Select("""
            SELECT * FROM pm_analysis_task
            WHERE id = #{taskId}
              AND status = 'RUNNING'
              AND worker_id = #{workerId}
              AND execution_version = #{executionVersion}
              AND lease_expires_at > NOW(6)
            FOR UPDATE
            """)
    AnalysisTask selectOwnedExecutionForUpdate(@Param("taskId") Long taskId,
                                                @Param("workerId") String workerId,
                                                @Param("executionVersion") Long executionVersion);

    @Update("""
            UPDATE pm_analysis_task
            SET status = 'SUCCESS', progress = 100, report_id = #{reportId},
                finish_time = NOW(6), active_key = NULL,
                worker_id = NULL, lease_expires_at = NULL, heartbeat_at = NULL,
                fail_reason = NULL, retry_reason = NULL
            WHERE id = #{taskId}
              AND status = 'RUNNING'
              AND worker_id = #{workerId}
              AND execution_version = #{executionVersion}
            """)
    int completeSuccess(@Param("taskId") Long taskId,
                        @Param("workerId") String workerId,
                        @Param("executionVersion") Long executionVersion,
                        @Param("reportId") Long reportId);

    @Update("""
            UPDATE pm_analysis_task
            SET status = 'PENDING', progress = 0,
                worker_id = NULL, lease_expires_at = NULL, heartbeat_at = NULL,
                retry_reason = #{reason}, fail_reason = NULL
            WHERE id = #{taskId}
              AND status = 'RUNNING'
              AND worker_id = #{workerId}
              AND execution_version = #{executionVersion}
            """)
    int releaseForRetry(@Param("taskId") Long taskId,
                        @Param("workerId") String workerId,
                        @Param("executionVersion") Long executionVersion,
                        @Param("reason") String reason);

    @Update("""
            UPDATE pm_analysis_task
            SET status = 'FAILED', progress = 100,
                finish_time = NOW(6), active_key = NULL,
                worker_id = NULL, lease_expires_at = NULL, heartbeat_at = NULL,
                fail_reason = #{reason}, retry_reason = #{reason}
            WHERE id = #{taskId}
              AND status = 'RUNNING'
              AND worker_id = #{workerId}
              AND execution_version = #{executionVersion}
            """)
    int failOwnedExecution(@Param("taskId") Long taskId,
                           @Param("workerId") String workerId,
                           @Param("executionVersion") Long executionVersion,
                           @Param("reason") String reason);

    @Select("""
            SELECT * FROM pm_analysis_task
            WHERE status = 'RUNNING' AND lease_expires_at <= NOW(6)
            ORDER BY lease_expires_at, id
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<AnalysisTask> selectExpiredExecutionsForUpdate(@Param("limit") int limit);

    @Update("""
            UPDATE pm_analysis_task
            SET status = 'PENDING', progress = 0,
                worker_id = NULL, lease_expires_at = NULL, heartbeat_at = NULL,
                retry_reason = #{reason}
            WHERE id = #{taskId}
              AND status = 'RUNNING'
              AND execution_version = #{executionVersion}
              AND lease_expires_at <= NOW(6)
            """)
    int recoverExpiredForRetry(@Param("taskId") Long taskId,
                               @Param("executionVersion") Long executionVersion,
                               @Param("reason") String reason);

    @Update("""
            UPDATE pm_analysis_task
            SET status = 'FAILED', progress = 100,
                finish_time = NOW(6), active_key = NULL,
                worker_id = NULL, lease_expires_at = NULL, heartbeat_at = NULL,
                fail_reason = #{reason}, retry_reason = #{reason}
            WHERE id = #{taskId}
              AND status = 'RUNNING'
              AND execution_version = #{executionVersion}
              AND lease_expires_at <= NOW(6)
            """)
    int failExpiredExecution(@Param("taskId") Long taskId,
                             @Param("executionVersion") Long executionVersion,
                             @Param("reason") String reason);

    @Update("""
            UPDATE pm_analysis_task
            SET status = 'FAILED', progress = 100,
                finish_time = NOW(6), active_key = NULL,
                fail_reason = #{reason}, retry_reason = #{reason}
            WHERE id = #{taskId} AND status = 'PENDING'
            """)
    int failPendingTask(@Param("taskId") Long taskId, @Param("reason") String reason);

    @Select("SELECT COUNT(*) FROM pm_analysis_task WHERE status = 'RUNNING'")
    long countRunning();
}

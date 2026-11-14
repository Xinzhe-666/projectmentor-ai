package com.xinzhe.projectmentor.indexing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface VectorIndexTaskMapper extends BaseMapper<VectorIndexTask> {
    @Select("SELECT * FROM pm_vector_index_task WHERE active_key=#{activeKey} LIMIT 1")
    VectorIndexTask selectActive(@Param("activeKey") String activeKey);

    @Update("""
            UPDATE pm_vector_index_task SET status='RUNNING', progress=GREATEST(progress,5),
              worker_id=#{workerId}, lease_expires_at=TIMESTAMPADD(SECOND,#{leaseSeconds},NOW(6)),
              heartbeat_at=NOW(6), execution_attempt=execution_attempt+1,
              execution_version=execution_version+1, last_message_id=#{messageId}, retry_reason=NULL
            WHERE id=#{taskId} AND user_id=#{userId} AND project_id=#{projectId} AND generation=#{generation}
              AND active_key IS NOT NULL AND execution_attempt < #{maximumAttempts}
              AND (status='PENDING' OR (status='RUNNING' AND lease_expires_at <= NOW(6)))
            """)
    int claim(@Param("taskId") long taskId, @Param("userId") long userId,
              @Param("projectId") long projectId, @Param("generation") long generation,
              @Param("workerId") String workerId, @Param("messageId") String messageId,
              @Param("leaseSeconds") int leaseSeconds, @Param("maximumAttempts") int maximumAttempts);

    @Update("""
            UPDATE pm_vector_index_task SET heartbeat_at=NOW(6),
              lease_expires_at=TIMESTAMPADD(SECOND,#{leaseSeconds},NOW(6)), progress=GREATEST(progress,#{progress})
            WHERE id=#{taskId} AND status='RUNNING' AND worker_id=#{workerId}
              AND execution_version=#{version} AND lease_expires_at > NOW(6)
            """)
    int heartbeat(@Param("taskId") long taskId, @Param("workerId") String workerId,
                  @Param("version") long version, @Param("leaseSeconds") int leaseSeconds,
                  @Param("progress") int progress);

    @Update("""
            UPDATE pm_vector_index_task SET status='SUCCEEDED', progress=100, active_key=NULL,
              worker_id=NULL, lease_expires_at=NULL, heartbeat_at=NULL, completed_at=NOW(6)
            WHERE id=#{taskId} AND status='RUNNING' AND worker_id=#{workerId}
              AND execution_version=#{version} AND lease_expires_at > NOW(6)
            """)
    int complete(@Param("taskId") long taskId, @Param("workerId") String workerId,
                 @Param("version") long version);

    @Update("""
            UPDATE pm_vector_index_task SET status='FAILED', progress=100, active_key=NULL,
              worker_id=NULL, lease_expires_at=NULL, heartbeat_at=NULL, completed_at=NOW(6),
              error_code=#{code}, error_message=#{message}, retry_reason=#{message}
            WHERE id=#{taskId} AND status='RUNNING' AND worker_id=#{workerId} AND execution_version=#{version}
            """)
    int failOwned(@Param("taskId") long taskId, @Param("workerId") String workerId,
                  @Param("version") long version, @Param("code") String code,
                  @Param("message") String message);

    @Update("""
            UPDATE pm_vector_index_task SET status='PENDING', worker_id=NULL, lease_expires_at=NULL,
              heartbeat_at=NULL, retry_reason=#{reason}
            WHERE id=#{taskId} AND status='RUNNING' AND worker_id=#{workerId} AND execution_version=#{version}
            """)
    int releaseForRetry(@Param("taskId") long taskId, @Param("workerId") String workerId,
                        @Param("version") long version, @Param("reason") String reason);

    @Select("""
            SELECT * FROM pm_vector_index_task WHERE status='RUNNING' AND lease_expires_at <= NOW(6)
            ORDER BY lease_expires_at,id LIMIT #{limit} FOR UPDATE SKIP LOCKED
            """)
    List<VectorIndexTask> selectExpiredForUpdate(@Param("limit") int limit);

    @Update("""
            UPDATE pm_vector_index_task SET status='PENDING', worker_id=NULL, lease_expires_at=NULL,
              heartbeat_at=NULL, retry_reason=#{reason}
            WHERE id=#{taskId} AND status='RUNNING' AND execution_version=#{version}
              AND lease_expires_at <= NOW(6)
            """)
    int recoverExpired(@Param("taskId") long taskId, @Param("version") long version,
                       @Param("reason") String reason);

    @Update("""
            UPDATE pm_vector_index_task SET status='FAILED', progress=100, active_key=NULL,
              worker_id=NULL, lease_expires_at=NULL, heartbeat_at=NULL, completed_at=NOW(6),
              error_code=#{code}, error_message=#{message}, retry_reason=#{message}
            WHERE id=#{taskId} AND status IN ('PENDING','RUNNING')
            """)
    int failTerminal(@Param("taskId") long taskId, @Param("code") String code,
                     @Param("message") String message);

    @Update("""
            UPDATE pm_vector_index_task SET status='CANCELLED', progress=100, active_key=NULL,
              worker_id=NULL, lease_expires_at=NULL, heartbeat_at=NULL, completed_at=NOW(6),
              error_code='PROJECT_DELETED', error_message='Project deleted', retry_reason='Project deleted'
            WHERE project_id=#{projectId} AND user_id=#{userId} AND status IN ('PENDING','RUNNING')
            """)
    int cancelActiveByProject(@Param("projectId") long projectId, @Param("userId") long userId);
}

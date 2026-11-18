package com.xinzhe.projectmentor.indexing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinzhe.projectmentor.indexing.entity.ProjectVectorIndex;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ProjectVectorIndexMapper extends BaseMapper<ProjectVectorIndex> {
    @Insert("""
            INSERT INTO pm_project_vector_index(project_id, user_id, status, collection_name)
            VALUES(#{projectId}, #{userId}, 'NOT_INDEXED', #{collection})
            ON DUPLICATE KEY UPDATE user_id = VALUES(user_id)
            """)
    int ensure(@Param("projectId") long projectId, @Param("userId") long userId,
               @Param("collection") String collection);

    @Select("SELECT * FROM pm_project_vector_index WHERE project_id=#{projectId} FOR UPDATE")
    ProjectVectorIndex selectForUpdate(@Param("projectId") long projectId);

    @Select("SELECT * FROM pm_project_vector_index WHERE project_id=#{projectId} AND user_id=#{userId}")
    ProjectVectorIndex selectOwned(@Param("projectId") long projectId, @Param("userId") long userId);

    @Update("""
            UPDATE pm_project_vector_index SET status='PENDING', pending_generation=#{generation},
              pending_build_id=NULL, reschedule_required=0,
              embedding_provider='openai-compatible', embedding_model=#{model}, embedding_dimension=#{dimension},
              last_error_code=NULL, last_error_message=NULL
            WHERE project_id=#{projectId} AND user_id=#{userId} AND source_revision=#{targetRevision}
            """)
    int markPending(@Param("projectId") long projectId, @Param("userId") long userId,
                    @Param("generation") long generation, @Param("targetRevision") long targetRevision,
                    @Param("model") String model,
                    @Param("dimension") int dimension);

    @Update("""
            UPDATE pm_project_vector_index SET status='RUNNING', pending_build_id=#{buildId}
            WHERE project_id=#{projectId} AND user_id=#{userId} AND pending_generation=#{generation}
              AND source_revision >= #{targetRevision}
              AND EXISTS (SELECT 1 FROM pm_vector_index_task t WHERE t.id=#{taskId}
                AND t.status='RUNNING' AND t.worker_id=#{workerId}
                AND t.execution_version=#{version} AND t.build_id=#{buildId})
            """)
    int markRunningFenced(@Param("projectId") long projectId, @Param("userId") long userId,
                          @Param("generation") long generation, @Param("targetRevision") long targetRevision,
                          @Param("taskId") long taskId, @Param("workerId") String workerId,
                          @Param("version") long version, @Param("buildId") String buildId);

    @Update("""
            UPDATE pm_project_vector_index SET status='READY', active_generation=#{generation},
              active_build_id=#{buildId}, pending_generation=NULL, pending_build_id=NULL,
              reschedule_required=0, chunk_count=#{chunkCount}, indexed_file_count=#{fileCount},
              last_error_code=NULL, last_error_message=NULL, last_indexed_at=NOW(6)
            WHERE project_id=#{projectId} AND user_id=#{userId} AND pending_generation=#{generation}
              AND pending_build_id=#{buildId} AND source_revision=#{targetRevision}
            """)
    int switchGeneration(@Param("projectId") long projectId, @Param("userId") long userId,
                         @Param("generation") long generation, @Param("buildId") String buildId,
                         @Param("targetRevision") long targetRevision, @Param("chunkCount") int chunkCount,
                         @Param("fileCount") int fileCount);

    @Update("""
            UPDATE pm_project_vector_index SET source_revision=source_revision+1, reschedule_required=1,
              status=CASE WHEN active_generation IS NOT NULL AND pending_generation IS NULL THEN 'DEGRADED' ELSE status END
            WHERE project_id=#{projectId} AND user_id=#{userId}
            """)
    int incrementSourceRevision(@Param("projectId") long projectId, @Param("userId") long userId);

    @Update("""
            UPDATE pm_project_vector_index SET status=IF(active_generation IS NULL,'FAILED','DEGRADED'),
              pending_generation=NULL, pending_build_id=NULL, last_error_code=#{code}, last_error_message=#{message}
            WHERE project_id=#{projectId} AND user_id=#{userId} AND pending_generation=#{generation}
              AND (#{buildId} IS NULL OR pending_build_id=#{buildId})
            """)
    int failPending(@Param("projectId") long projectId, @Param("userId") long userId,
                    @Param("generation") long generation, @Param("buildId") String buildId,
                    @Param("code") String code,
                    @Param("message") String message);

    @Update("UPDATE pm_project_vector_index SET status='DELETING' WHERE project_id=#{projectId} AND user_id=#{userId}")
    int markDeleting(@Param("projectId") long projectId, @Param("userId") long userId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM pm_project_vector_index WHERE project_id=#{projectId} AND user_id=#{userId}")
    int deleteOwned(@Param("projectId") long projectId, @Param("userId") long userId);
}

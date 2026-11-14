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
              embedding_provider='openai-compatible', embedding_model=#{model}, embedding_dimension=#{dimension},
              last_error_code=NULL, last_error_message=NULL
            WHERE project_id=#{projectId} AND user_id=#{userId}
            """)
    int markPending(@Param("projectId") long projectId, @Param("userId") long userId,
                    @Param("generation") long generation, @Param("model") String model,
                    @Param("dimension") int dimension);

    @Update("""
            UPDATE pm_project_vector_index SET status='RUNNING'
            WHERE project_id=#{projectId} AND user_id=#{userId} AND pending_generation=#{generation}
            """)
    int markRunning(@Param("projectId") long projectId, @Param("userId") long userId,
                    @Param("generation") long generation);

    @Update("""
            UPDATE pm_project_vector_index SET status='READY', active_generation=#{generation},
              pending_generation=NULL, chunk_count=#{chunkCount}, indexed_file_count=#{fileCount},
              last_error_code=NULL, last_error_message=NULL, last_indexed_at=NOW(6)
            WHERE project_id=#{projectId} AND user_id=#{userId} AND pending_generation=#{generation}
            """)
    int switchGeneration(@Param("projectId") long projectId, @Param("userId") long userId,
                         @Param("generation") long generation, @Param("chunkCount") int chunkCount,
                         @Param("fileCount") int fileCount);

    @Update("""
            UPDATE pm_project_vector_index SET status=IF(active_generation IS NULL,'FAILED','DEGRADED'),
              pending_generation=NULL, last_error_code=#{code}, last_error_message=#{message}
            WHERE project_id=#{projectId} AND user_id=#{userId} AND pending_generation=#{generation}
            """)
    int failPending(@Param("projectId") long projectId, @Param("userId") long userId,
                    @Param("generation") long generation, @Param("code") String code,
                    @Param("message") String message);

    @Update("UPDATE pm_project_vector_index SET status='DELETING' WHERE project_id=#{projectId} AND user_id=#{userId}")
    int markDeleting(@Param("projectId") long projectId, @Param("userId") long userId);
}

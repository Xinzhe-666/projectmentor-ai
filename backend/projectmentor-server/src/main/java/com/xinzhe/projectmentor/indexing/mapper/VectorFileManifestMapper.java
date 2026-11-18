package com.xinzhe.projectmentor.indexing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinzhe.projectmentor.indexing.entity.VectorFileManifest;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface VectorFileManifestMapper extends BaseMapper<VectorFileManifest> {
    @Delete("DELETE FROM pm_vector_file_manifest WHERE project_id=#{projectId} AND generation=#{generation} AND build_id=#{buildId}")
    int deleteBuild(@Param("projectId") long projectId, @Param("generation") long generation,
                    @Param("buildId") String buildId);

    @Delete("DELETE FROM pm_vector_file_manifest WHERE project_id=#{projectId}")
    int deleteProject(@Param("projectId") long projectId);
}

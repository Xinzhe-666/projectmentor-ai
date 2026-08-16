package com.xinzhe.projectmentor.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinzhe.projectmentor.file.entity.ProjectFile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ProjectFileMapper extends BaseMapper<ProjectFile> {

    @Insert("""
            INSERT INTO pm_project_file (project_id, file_path, file_type, content)
            VALUES (#{projectId}, #{filePath}, #{fileType}, #{content})
            ON DUPLICATE KEY UPDATE
                file_type = VALUES(file_type),
                content = VALUES(content),
                update_time = CURRENT_TIMESTAMP
            """)
    int upsertProjectFile(ProjectFile projectFile);

    @Select("""
            SELECT id, project_id, file_path, file_type, content, create_time, update_time
            FROM pm_project_file
            WHERE project_id = #{projectId}
              AND file_path = #{filePath}
            LIMIT 1
            """)
    ProjectFile selectByProjectIdAndFilePath(@Param("projectId") Long projectId,
                                             @Param("filePath") String filePath);
}

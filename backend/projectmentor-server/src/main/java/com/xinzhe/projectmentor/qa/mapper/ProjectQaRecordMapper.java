package com.xinzhe.projectmentor.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinzhe.projectmentor.qa.entity.ProjectQaRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface ProjectQaRecordMapper extends BaseMapper<ProjectQaRecord> {

    @Delete("DELETE FROM pm_project_qa_record WHERE project_id = #{projectId}")
    int physicalDeleteByProjectId(@Param("projectId") Long projectId);
}

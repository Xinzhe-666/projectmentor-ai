package com.xinzhe.projectmentor.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
}

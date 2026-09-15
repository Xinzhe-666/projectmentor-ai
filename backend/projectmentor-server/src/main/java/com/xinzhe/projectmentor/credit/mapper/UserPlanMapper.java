package com.xinzhe.projectmentor.credit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinzhe.projectmentor.credit.entity.UserPlan;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserPlanMapper extends BaseMapper<UserPlan> {

    @Select("""
            SELECT id, user_id, plan_type, remaining_credits, expire_time, create_time, update_time
            FROM pm_user_plan
            WHERE user_id = #{userId}
            LIMIT 1
            FOR UPDATE
            """)
    UserPlan selectByUserIdForUpdate(@Param("userId") Long userId);
}

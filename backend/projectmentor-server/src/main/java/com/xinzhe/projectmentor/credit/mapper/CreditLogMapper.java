package com.xinzhe.projectmentor.credit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinzhe.projectmentor.admin.vo.AdminAiUsageLogVO;
import com.xinzhe.projectmentor.admin.vo.AdminAiUsageModuleVO;
import com.xinzhe.projectmentor.admin.vo.AdminAiUsageUserVO;
import com.xinzhe.projectmentor.admin.vo.AiUsageAggregateRow;
import com.xinzhe.projectmentor.credit.entity.CreditLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface CreditLogMapper extends BaseMapper<CreditLog> {

    @Select("""
            <script>
            SELECT
                COALESCE(SUM(CASE
                    WHEN change_amount &lt; 0 AND RIGHT(operation_type, 7) != '_REFUND' THEN 1
                    ELSE 0
                END), 0) AS ai_calls,
                COALESCE(SUM(CASE
                    WHEN change_amount &lt; 0 AND RIGHT(operation_type, 7) != '_REFUND' THEN -change_amount
                    ELSE 0
                END), 0) AS credits_consumed,
                COALESCE(SUM(CASE
                    WHEN change_amount &gt; 0 AND RIGHT(operation_type, 7) = '_REFUND' THEN 1
                    ELSE 0
                END), 0) AS refund_count,
                COALESCE(SUM(CASE
                    WHEN change_amount &gt; 0 AND RIGHT(operation_type, 7) = '_REFUND' THEN change_amount
                    ELSE 0
                END), 0) AS refund_credits
            FROM pm_credit_log
            WHERE LEFT(operation_type, 3) = 'AI_'
            <if test="startTime != null">
                AND create_time &gt;= #{startTime}
            </if>
            </script>
            """)
    AiUsageAggregateRow selectAiUsageAggregate(@Param("startTime") LocalDateTime startTime);

    @Select("""
            SELECT
                operation_type AS module,
                COUNT(*) AS ai_calls,
                COALESCE(SUM(-change_amount), 0) AS credits_consumed
            FROM pm_credit_log
            WHERE LEFT(operation_type, 3) = 'AI_'
              AND RIGHT(operation_type, 7) != '_REFUND'
              AND change_amount < 0
              AND create_time >= #{startTime}
            GROUP BY operation_type
            ORDER BY credits_consumed DESC, ai_calls DESC
            LIMIT 10
            """)
    List<AdminAiUsageModuleVO> selectTopAiModules(@Param("startTime") LocalDateTime startTime);

    @Select("""
            SELECT
                l.user_id,
                u.username,
                u.email,
                COUNT(*) AS ai_calls,
                COALESCE(SUM(-l.change_amount), 0) AS credits_consumed
            FROM pm_credit_log l
            LEFT JOIN pm_user u ON u.id = l.user_id
            WHERE LEFT(l.operation_type, 3) = 'AI_'
              AND RIGHT(l.operation_type, 7) != '_REFUND'
              AND l.change_amount < 0
              AND l.create_time >= #{startTime}
            GROUP BY l.user_id, u.username, u.email
            ORDER BY credits_consumed DESC, ai_calls DESC
            LIMIT 10
            """)
    List<AdminAiUsageUserVO> selectTopAiUsers(@Param("startTime") LocalDateTime startTime);

    @Select("""
            SELECT
                l.id,
                l.user_id,
                u.username,
                l.change_amount AS amount,
                CASE
                    WHEN RIGHT(l.operation_type, 7) = '_REFUND' THEN 'REFUND'
                    ELSE 'CONSUME'
                END AS type,
                l.operation_type AS module,
                l.remark AS description,
                l.after_amount AS balance_after,
                l.create_time AS created_at
            FROM pm_credit_log l
            LEFT JOIN pm_user u ON u.id = l.user_id
            WHERE LEFT(l.operation_type, 3) = 'AI_'
            ORDER BY l.create_time DESC, l.id DESC
            LIMIT 10
            """)
    List<AdminAiUsageLogVO> selectRecentAiCreditLogs();
}

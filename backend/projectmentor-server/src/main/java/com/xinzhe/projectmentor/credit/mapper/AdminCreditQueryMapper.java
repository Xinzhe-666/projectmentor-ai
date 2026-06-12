package com.xinzhe.projectmentor.credit.mapper;

import com.xinzhe.projectmentor.credit.vo.AdminCreditUserVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AdminCreditQueryMapper {

    @Select("""
            <script>
            SELECT
                u.id AS user_id,
                u.username,
                u.email,
                COALESCE(p.credit_balance, 0) AS credit_balance,
                COALESCE(l.total_consumed, 0) AS total_consumed,
                COALESCE(l.total_refunded, 0) AS total_refunded,
                COALESCE(l.total_admin_granted, 0) AS total_admin_granted,
                u.create_time AS created_at,
                l.last_credit_change_at
            FROM pm_user u
            LEFT JOIN (
                SELECT user_id, MAX(remaining_credits) AS credit_balance
                FROM pm_user_plan
                GROUP BY user_id
            ) p ON p.user_id = u.id
            LEFT JOIN (
                SELECT
                    user_id,
                    SUM(CASE WHEN change_amount &lt; 0 THEN -change_amount ELSE 0 END) AS total_consumed,
                    SUM(CASE WHEN change_amount &gt; 0 AND operation_type LIKE '%REFUND%' THEN change_amount ELSE 0 END) AS total_refunded,
                    SUM(CASE WHEN change_amount &gt; 0 AND operation_type = 'ADMIN_GRANT' THEN change_amount ELSE 0 END) AS total_admin_granted,
                    MAX(create_time) AS last_credit_change_at
                FROM pm_credit_log
                GROUP BY user_id
            ) l ON l.user_id = u.id
            <where>
                <if test="keyword != null and keyword != ''">
                    (
                        u.username LIKE CONCAT('%', #{keyword}, '%')
                        OR u.email LIKE CONCAT('%', #{keyword}, '%')
                        OR CAST(u.id AS CHAR) = #{keyword}
                    )
                </if>
            </where>
            ORDER BY ${sortColumn} ${sortDirection}, u.id DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<AdminCreditUserVO> selectCreditUsers(@Param("keyword") String keyword,
                                               @Param("offset") int offset,
                                               @Param("size") int size,
                                               @Param("sortColumn") String sortColumn,
                                               @Param("sortDirection") String sortDirection);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM pm_user u
            <where>
                <if test="keyword != null and keyword != ''">
                    (
                        u.username LIKE CONCAT('%', #{keyword}, '%')
                        OR u.email LIKE CONCAT('%', #{keyword}, '%')
                        OR CAST(u.id AS CHAR) = #{keyword}
                    )
                </if>
            </where>
            </script>
            """)
    long countCreditUsers(@Param("keyword") String keyword);
}

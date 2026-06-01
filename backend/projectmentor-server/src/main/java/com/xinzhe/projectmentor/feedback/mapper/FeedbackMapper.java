package com.xinzhe.projectmentor.feedback.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinzhe.projectmentor.feedback.entity.Feedback;
import com.xinzhe.projectmentor.feedback.vo.AdminFeedbackVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FeedbackMapper extends BaseMapper<Feedback> {

    @Select("""
            <script>
            SELECT
                f.id,
                f.user_id AS userId,
                u.email AS userEmail,
                f.type,
                f.content,
                f.contact,
                f.page_url AS pageUrl,
                f.status,
                f.admin_note AS adminNote,
                f.create_time AS createTime,
                f.update_time AS updateTime
            FROM pm_feedback f
            LEFT JOIN pm_user u ON u.id = f.user_id
            WHERE f.deleted = 0
            <if test="type != null and type != ''">
                AND f.type = #{type}
            </if>
            <if test="status != null and status != ''">
                AND f.status = #{status}
            </if>
            <if test="keyword != null and keyword != ''">
                AND (
                    f.content LIKE CONCAT('%', #{keyword}, '%')
                    OR f.contact LIKE CONCAT('%', #{keyword}, '%')
                    OR u.email LIKE CONCAT('%', #{keyword}, '%')
                )
            </if>
            ORDER BY f.create_time DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<AdminFeedbackVO> selectAdminFeedbackPage(@Param("type") String type,
                                                  @Param("status") String status,
                                                  @Param("keyword") String keyword,
                                                  @Param("offset") Integer offset,
                                                  @Param("size") Integer size);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM pm_feedback f
            LEFT JOIN pm_user u ON u.id = f.user_id
            WHERE f.deleted = 0
            <if test="type != null and type != ''">
                AND f.type = #{type}
            </if>
            <if test="status != null and status != ''">
                AND f.status = #{status}
            </if>
            <if test="keyword != null and keyword != ''">
                AND (
                    f.content LIKE CONCAT('%', #{keyword}, '%')
                    OR f.contact LIKE CONCAT('%', #{keyword}, '%')
                    OR u.email LIKE CONCAT('%', #{keyword}, '%')
                )
            </if>
            </script>
            """)
    Long countAdminFeedback(@Param("type") String type,
                            @Param("status") String status,
                            @Param("keyword") String keyword);

    @Select("""
            SELECT
                f.id,
                f.user_id AS userId,
                u.email AS userEmail,
                f.type,
                f.content,
                f.contact,
                f.page_url AS pageUrl,
                f.status,
                f.admin_note AS adminNote,
                f.create_time AS createTime,
                f.update_time AS updateTime
            FROM pm_feedback f
            LEFT JOIN pm_user u ON u.id = f.user_id
            WHERE f.deleted = 0 AND f.id = #{id}
            LIMIT 1
            """)
    AdminFeedbackVO selectAdminFeedbackById(@Param("id") Long id);
}

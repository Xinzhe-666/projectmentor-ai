package com.xinzhe.projectmentor.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinzhe.projectmentor.analysis.entity.AnalysisOutboxEvent;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface AnalysisOutboxMapper extends BaseMapper<AnalysisOutboxEvent> {

    @Select("""
            SELECT *
            FROM pm_analysis_outbox
            WHERE (status = 'NEW' AND next_attempt_at <= NOW(6))
               OR (status = 'CLAIMED' AND claim_expires_at <= NOW(6))
            ORDER BY id
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<AnalysisOutboxEvent> selectClaimableForUpdate(@Param("limit") int limit);

    @Update("""
            UPDATE pm_analysis_outbox
            SET status = 'CLAIMED',
                claim_owner = #{claimOwner},
                claim_expires_at = TIMESTAMPADD(SECOND, #{claimLeaseSeconds}, NOW(6)),
                publish_attempt = publish_attempt + 1,
                last_error = NULL
            WHERE id = #{id}
              AND ((status = 'NEW' AND next_attempt_at <= NOW(6))
                OR (status = 'CLAIMED' AND claim_expires_at <= NOW(6)))
            """)
    int claim(@Param("id") Long id,
              @Param("claimOwner") String claimOwner,
              @Param("claimLeaseSeconds") int claimLeaseSeconds);

    @Update("""
            UPDATE pm_analysis_outbox
            SET status = 'PUBLISHED',
                published_at = NOW(6),
                claim_owner = NULL,
                claim_expires_at = NULL,
                last_error = NULL
            WHERE id = #{id} AND status = 'CLAIMED' AND claim_owner = #{claimOwner}
            """)
    int markPublished(@Param("id") Long id, @Param("claimOwner") String claimOwner);

    @Update("""
            UPDATE pm_analysis_outbox
            SET status = 'NEW',
                next_attempt_at = TIMESTAMPADD(SECOND, #{delaySeconds}, NOW(6)),
                claim_owner = NULL,
                claim_expires_at = NULL,
                last_error = #{error}
            WHERE id = #{id} AND status = 'CLAIMED' AND claim_owner = #{claimOwner}
            """)
    int reschedule(@Param("id") Long id,
                   @Param("claimOwner") String claimOwner,
                   @Param("delaySeconds") int delaySeconds,
                   @Param("error") String error);

    @Update("""
            UPDATE pm_analysis_outbox
            SET status = 'FAILED',
                claim_owner = NULL,
                claim_expires_at = NULL,
                last_error = #{error}
            WHERE id = #{id} AND status = 'CLAIMED' AND claim_owner = #{claimOwner}
            """)
    int markFailed(@Param("id") Long id,
                   @Param("claimOwner") String claimOwner,
                   @Param("error") String error);

    @Select("SELECT COUNT(*) FROM pm_analysis_outbox WHERE status IN ('NEW', 'CLAIMED')")
    long countBacklog();

    @Delete("""
            DELETE FROM pm_analysis_outbox
            WHERE status = 'PUBLISHED'
              AND published_at < TIMESTAMPADD(HOUR, -#{retentionHours}, NOW(6))
            """)
    int deletePublishedOlderThan(@Param("retentionHours") int retentionHours);
}

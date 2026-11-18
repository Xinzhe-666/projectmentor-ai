package com.xinzhe.projectmentor.indexing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexOutboxEvent;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface VectorIndexOutboxMapper extends BaseMapper<VectorIndexOutboxEvent> {
    @Select("SELECT COUNT(*) FROM pm_vector_index_outbox WHERE status IN ('NEW','CLAIMED')")
    long countBacklog();

    @Select("""
            SELECT * FROM pm_vector_index_outbox
            WHERE (status='NEW' OR (status='CLAIMED' AND claim_expires_at <= NOW(6)))
              AND next_attempt_at <= NOW(6) ORDER BY id LIMIT #{limit} FOR UPDATE SKIP LOCKED
            """)
    List<VectorIndexOutboxEvent> selectClaimable(@Param("limit") int limit);

    @Update("""
            UPDATE pm_vector_index_outbox SET status='CLAIMED', claim_owner=#{owner},
              claim_expires_at=TIMESTAMPADD(SECOND,#{leaseSeconds},NOW(6)), publish_attempt=publish_attempt+1
            WHERE id=#{id} AND (status='NEW' OR (status='CLAIMED' AND claim_expires_at <= NOW(6)))
            """)
    int claim(@Param("id") long id, @Param("owner") String owner, @Param("leaseSeconds") int leaseSeconds);

    @Update("""
            UPDATE pm_vector_index_outbox SET status='PUBLISHED', published_at=NOW(6),
              claim_owner=NULL, claim_expires_at=NULL, last_error=NULL
            WHERE id=#{id} AND status='CLAIMED' AND claim_owner=#{owner}
            """)
    int markPublished(@Param("id") long id, @Param("owner") String owner);

    @Update("""
            UPDATE pm_vector_index_outbox SET status='NEW', next_attempt_at=TIMESTAMPADD(SECOND,#{delay},NOW(6)),
              claim_owner=NULL, claim_expires_at=NULL, last_error=#{reason}
            WHERE id=#{id} AND status='CLAIMED' AND claim_owner=#{owner}
            """)
    int reschedule(@Param("id") long id, @Param("owner") String owner,
                   @Param("delay") int delay, @Param("reason") String reason);

    @Update("""
            UPDATE pm_vector_index_outbox SET status='PUBLISHED', published_at=NOW(6), last_error=NULL
            WHERE id=#{id} AND status='NEW'
            """)
    int markLocalPublished(@Param("id") long id);

    @Update("""
            UPDATE pm_vector_index_outbox SET status='FAILED', claim_owner=NULL, claim_expires_at=NULL,
              last_error=#{reason}
            WHERE id=#{id} AND status='CLAIMED' AND claim_owner=#{owner}
            """)
    int markFailed(@Param("id") long id, @Param("owner") String owner, @Param("reason") String reason);
}

package com.xinzhe.projectmentor.analysis;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisConcurrencySchemaMigrationTests {

    @Test
    void migrationAddsNullableActiveKeyAndSingleColumnUniqueIndex() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/db/migration/V4__analysis_concurrency_foundation.sql"
        )) {
            assertThat(input).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("ALTER TABLE pm_analysis_task")
                    .contains("active_key VARCHAR(100) NULL")
                    .contains("UNIQUE KEY uk_analysis_task_active_key (active_key)");
        }
    }

    @Test
    void v5AddsOutboxLeasesFencingAndDatabaseIdempotencyConstraints() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/db/migration/V5__reliable_analysis_pipeline.sql"
        )) {
            assertThat(input).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE pm_analysis_outbox")
                    .contains("UNIQUE KEY uk_analysis_outbox_event_id (event_id)")
                    .contains("INDEX idx_analysis_outbox_relay (status, next_attempt_at, claim_expires_at, id)")
                    .contains("execution_version BIGINT NOT NULL DEFAULT 0")
                    .contains("lease_expires_at DATETIME(6) NULL")
                    .contains("UNIQUE KEY uk_credit_log_idempotency_key (idempotency_key)")
                    .contains("UNIQUE KEY uk_analysis_report_task_id (task_id)");
        }
    }
}

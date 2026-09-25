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
}

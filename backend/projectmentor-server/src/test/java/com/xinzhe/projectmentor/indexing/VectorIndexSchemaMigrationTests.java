package com.xinzhe.projectmentor.indexing;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class VectorIndexSchemaMigrationTests {
    @Test
    void v6AddsGenerationTasksManifestAndDedicatedOutbox() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/db/migration/V6__project_vector_index.sql")) {
            assertThat(input).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql)
                    .contains("CREATE TABLE pm_project_vector_index")
                    .contains("CREATE TABLE pm_vector_index_task")
                    .contains("CREATE TABLE pm_vector_file_manifest")
                    .contains("CREATE TABLE pm_vector_index_outbox")
                    .contains("UNIQUE KEY uk_vector_task_active (active_key)")
                    .contains("execution_version BIGINT")
                    .contains("lease_expires_at")
                    .contains("active_generation")
                    .contains("pending_generation")
                    .contains("file_path          VARCHAR(255) NOT NULL")
                    .contains("UNIQUE KEY uk_vector_manifest_file (project_id, generation, file_path)");
        }
    }
}

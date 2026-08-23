package com.xinzhe.projectmentor.defense;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DefenseSchemaMigrationTests {

    @Test
    void migrationContainsDefenseTablesAndCoreConstraints() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/db/migration/V3__defense_domain.sql"
        )) {
            assertThat(input).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE IF NOT EXISTS pm_defense_session")
                    .contains("CREATE TABLE IF NOT EXISTS pm_defense_question")
                    .contains("CREATE TABLE IF NOT EXISTS pm_defense_answer")
                    .contains("UNIQUE KEY uk_defense_question_order (session_id, sort_order)")
                    .contains("UNIQUE KEY uk_defense_answer_question (question_id)")
                    .contains("ON DELETE CASCADE");
        }
    }
}

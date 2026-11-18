package com.xinzhe.projectmentor.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.message.VectorIndexMessage;
import com.xinzhe.projectmentor.indexing.service.VectorIndexDeletionService;
import com.xinzhe.projectmentor.indexing.service.VectorIndexOutboxRelay;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "projectmentor.vector-index.dispatch-mode=rabbit",
        "projectmentor.vector-index.outbox-poll-interval-millis=3600000",
        "projectmentor.vector-index.recovery-scan-interval-millis=3600000",
        "projectmentor.milvus.enabled=false",
        "projectmentor.embedding.enabled=false",
        "projectmentor.ai.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class VectorIndexRabbitIT {
    private static final long USER_ID = 91001;
    private static final long PROJECT_ID = 92001;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ProjectVectorIndexMapper indexes;
    @Autowired private VectorIndexDeletionService deletions;
    @Autowired private VectorIndexOutboxRelay relay;
    @Autowired private VectorIndexProperties properties;
    @Autowired private RabbitTemplate rabbit;
    @Autowired private RabbitAdmin admin;
    @Autowired private ObjectMapper json;

    @BeforeEach
    void setUp() {
        cleanup();
        admin.purgeQueue(properties.getMainQueue(), true);
        indexes.ensure(PROJECT_ID, USER_ID, "pmai_code_chunks_v1");
    }

    @AfterEach
    void tearDown() { cleanup(); }

    @Test
    void committedProjectDeleteOutboxIsPublishedByRecoveredRelay() throws Exception {
        deletions.request(USER_ID, PROJECT_ID);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pm_vector_index_outbox WHERE event_type='PROJECT_DELETE'",
                Integer.class)).isEqualTo(1);
        assertThat(rabbit.receive(properties.getMainQueue(), 100)).isNull();

        relay.relay();

        Message delivery = rabbit.receive(properties.getMainQueue(), 5000);
        assertThat(delivery).isNotNull();
        VectorIndexMessage message = json.readValue(delivery.getBody(), VectorIndexMessage.class);
        assertThat(message.eventType()).isEqualTo("PROJECT_DELETE");
        assertThat(message.userId()).isEqualTo(USER_ID);
        assertThat(message.projectId()).isEqualTo(PROJECT_ID);
        assertThat(jdbc.queryForObject("SELECT status FROM pm_vector_index_outbox WHERE event_type='PROJECT_DELETE'",
                String.class)).isEqualTo("PUBLISHED");
    }

    private void cleanup() {
        try {
            jdbc.update("DELETE o FROM pm_vector_index_outbox o JOIN pm_vector_index_task t ON t.id=o.task_id WHERE t.project_id=?", PROJECT_ID);
            jdbc.update("DELETE FROM pm_vector_index_task WHERE project_id=?", PROJECT_ID);
            jdbc.update("DELETE FROM pm_vector_file_manifest WHERE project_id=?", PROJECT_ID);
            jdbc.update("DELETE FROM pm_project_vector_index WHERE project_id=?", PROJECT_ID);
        } catch (Exception ignored) {
            // Tables do not exist before Flyway has initialized the integration database.
        }
    }
}

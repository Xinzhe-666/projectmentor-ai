package com.xinzhe.projectmentor.integration;

import com.xinzhe.projectmentor.indexing.entity.ProjectVectorIndex;
import com.xinzhe.projectmentor.indexing.entity.VectorFileManifest;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexTask;
import com.xinzhe.projectmentor.indexing.mapper.ProjectVectorIndexMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorFileManifestMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "projectmentor.milvus.enabled=false",
        "projectmentor.embedding.enabled=false",
        "projectmentor.ai.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class VectorIndexMySqlIT {
    private static final String PREFIX = "pmai_vector_it_";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ProjectVectorIndexMapper indexes;
    @Autowired private VectorIndexTaskMapper tasks;
    @Autowired private VectorFileManifestMapper manifests;

    private long userId;
    private long projectId;

    @BeforeEach
    void setUp() {
        cleanup();
        jdbc.update("INSERT INTO pm_user(username,password,email) VALUES (?,?,?)",
                PREFIX + "user", "not-a-real-password", PREFIX + "user@example.invalid");
        userId = jdbc.queryForObject("SELECT id FROM pm_user WHERE username=?", Long.class, PREFIX + "user");
        jdbc.update("INSERT INTO pm_project(user_id,name,status) VALUES (?,?, 'PENDING')",
                userId, PREFIX + "project");
        projectId = jdbc.queryForObject("SELECT id FROM pm_project WHERE name=?", Long.class, PREFIX + "project");
        indexes.ensure(projectId, userId, "pmai_code_chunks_v1");
    }

    @AfterEach
    void tearDown() { cleanup(); }

    @Test
    void recoveredClaimHasNewBuildAndOldFenceCannotMutateActivePointerOrManifest() {
        VectorIndexTask task = task(1, 0);
        tasks.insert(task);
        assertThat(indexes.markPending(projectId, userId, 1, 0, "test-model", 4)).isEqualTo(1);

        assertThat(tasks.claim(task.getId(), userId, projectId, 1, "worker-a", "build-a",
                "message-a", 30, 4)).isEqualTo(1);
        VectorIndexTask attemptA = tasks.selectById(task.getId());
        assertThat(indexes.markRunningFenced(projectId, userId, 1, 0, task.getId(), "worker-a",
                attemptA.getExecutionVersion(), "build-a")).isEqualTo(1);

        jdbc.update("UPDATE pm_vector_index_task SET lease_expires_at=DATE_SUB(NOW(6), INTERVAL 1 SECOND) WHERE id=?",
                task.getId());
        assertThat(tasks.recoverExpired(task.getId(), attemptA.getExecutionVersion(), "controlled takeover"))
                .isEqualTo(1);
        assertThat(tasks.claim(task.getId(), userId, projectId, 1, "worker-b", "build-b",
                "message-b", 30, 4)).isEqualTo(1);
        VectorIndexTask attemptB = tasks.selectById(task.getId());
        assertThat(indexes.markRunningFenced(projectId, userId, 1, 0, task.getId(), "worker-b",
                attemptB.getExecutionVersion(), "build-b")).isEqualTo(1);

        manifests.insert(manifest("build-a", "src/A.java"));
        manifests.insert(manifest("build-b", "src/B.java"));
        assertThat(manifests.deleteBuild(projectId, 1, "build-a")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pm_vector_file_manifest WHERE project_id=? AND build_id='build-b'",
                Integer.class, projectId)).isEqualTo(1);

        assertThat(tasks.complete(task.getId(), "worker-a", attemptA.getExecutionVersion(), "build-a")).isZero();
        assertThat(indexes.switchGeneration(projectId, userId, 1, "build-a", 0, 1, 1)).isZero();
        assertThat(tasks.complete(task.getId(), "worker-b", attemptB.getExecutionVersion(), "build-b")).isEqualTo(1);
        assertThat(indexes.switchGeneration(projectId, userId, 1, "build-b", 0, 1, 1)).isEqualTo(1);
        ProjectVectorIndex state = indexes.selectOwned(projectId, userId);
        assertThat(state.getActiveGeneration()).isEqualTo(1);
        assertThat(state.getActiveBuildId()).isEqualTo("build-b");
    }

    @Test
    void contentChangesPersistRevisionAndRescheduleSignalWithoutCreatingDuplicateActiveTask() {
        VectorIndexTask task = task(1, 0);
        tasks.insert(task);
        indexes.markPending(projectId, userId, 1, 0, "test-model", 4);

        assertThat(indexes.incrementSourceRevision(projectId, userId)).isEqualTo(1);
        assertThat(indexes.incrementSourceRevision(projectId, userId)).isEqualTo(1);

        ProjectVectorIndex state = indexes.selectOwned(projectId, userId);
        assertThat(state.getSourceRevision()).isEqualTo(2);
        assertThat(state.getRescheduleRequired()).isTrue();
        assertThat(tasks.countActiveTasks()).isEqualTo(1);
        assertThat(tasks.countDuplicateActiveProjects()).isZero();
    }

    private VectorIndexTask task(long generation, long targetRevision) {
        VectorIndexTask task = new VectorIndexTask();
        task.setProjectId(projectId); task.setUserId(userId); task.setTaskType("REBUILD");
        task.setActiveKey("project:" + projectId); task.setCorrelationId(UUID.randomUUID().toString());
        task.setGeneration(generation); task.setTargetRevision(targetRevision); task.setStatus("PENDING");
        task.setProgress(0); task.setExecutionAttempt(0); task.setExecutionVersion(0L);
        return task;
    }

    private VectorFileManifest manifest(String build, String path) {
        VectorFileManifest manifest = new VectorFileManifest();
        manifest.setProjectId(projectId); manifest.setUserId(userId); manifest.setFilePath(path);
        manifest.setContentHash("a".repeat(64)); manifest.setGeneration(1L); manifest.setBuildId(build);
        manifest.setEmbeddingModel("test-model"); manifest.setChunkCount(1); manifest.setIndexedAt(LocalDateTime.now());
        return manifest;
    }

    private void cleanup() {
        try {
            jdbc.update("DELETE o FROM pm_vector_index_outbox o JOIN pm_vector_index_task t ON t.id=o.task_id WHERE t.project_id IN (SELECT id FROM pm_project WHERE name LIKE ?)", PREFIX + "%");
            jdbc.update("DELETE FROM pm_vector_file_manifest WHERE project_id IN (SELECT id FROM pm_project WHERE name LIKE ?)", PREFIX + "%");
            jdbc.update("DELETE FROM pm_vector_index_task WHERE project_id IN (SELECT id FROM pm_project WHERE name LIKE ?)", PREFIX + "%");
            jdbc.update("DELETE FROM pm_project_vector_index WHERE project_id IN (SELECT id FROM pm_project WHERE name LIKE ?)", PREFIX + "%");
            jdbc.update("DELETE FROM pm_project WHERE name LIKE ?", PREFIX + "%");
            jdbc.update("DELETE FROM pm_user WHERE username LIKE ?", PREFIX + "%");
        } catch (Exception ignored) {
            // Tables do not exist before Flyway has initialized the integration database.
        }
    }
}

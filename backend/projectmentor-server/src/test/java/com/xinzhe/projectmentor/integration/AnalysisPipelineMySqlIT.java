package com.xinzhe.projectmentor.integration;

import com.xinzhe.projectmentor.analysis.entity.AnalysisOutboxEvent;
import com.xinzhe.projectmentor.analysis.entity.AnalysisReport;
import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisOutboxMapper;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisTaskMapper;
import com.xinzhe.projectmentor.analysis.service.AnalysisExecutionTransitionService;
import com.xinzhe.projectmentor.analysis.service.AnalysisExecutionContext;
import com.xinzhe.projectmentor.analysis.service.AnalysisOutboxClaimService;
import com.xinzhe.projectmentor.analysis.service.AnalysisReportPersistenceService;
import com.xinzhe.projectmentor.analysis.service.AnalysisTaskSubmissionService;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import com.xinzhe.projectmentor.credit.service.CreditService;
import com.xinzhe.projectmentor.project.entity.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "projectmentor.analysis.dispatch-mode=rabbit",
        "projectmentor.analysis.rabbit.outbox.poll-interval-millis=3600000",
        "projectmentor.analysis.rabbit.execution.recovery-scan-interval-millis=3600000",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "projectmentor.ai.enabled=false"
})
class AnalysisPipelineMySqlIT {

    private static final String PREFIX = "pmai_it_";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private AnalysisTaskSubmissionService submissionService;
    @Autowired private AnalysisTaskMapper taskMapper;
    @Autowired private AnalysisOutboxMapper outboxMapper;
    @Autowired private AnalysisOutboxClaimService claimService;
    @Autowired private AnalysisExecutionTransitionService transitions;
    @Autowired private AnalysisPipelineProperties properties;
    @Autowired private CreditService creditService;
    @Autowired private AnalysisReportPersistenceService reportPersistenceService;

    private Long userId;
    private Long projectId;

    @BeforeEach
    void setUp() {
        cleanup();
        jdbc.update("INSERT INTO pm_user(username,password,email) VALUES (?,?,?)",
                PREFIX + "user", "not-a-real-password", PREFIX + "user@example.invalid");
        userId = jdbc.queryForObject("SELECT id FROM pm_user WHERE username=?", Long.class, PREFIX + "user");
        jdbc.update("INSERT INTO pm_user_plan(user_id,plan_type,remaining_credits) VALUES (?,'FREE',10)", userId);
        jdbc.update("INSERT INTO pm_project(user_id,name,status) VALUES (?,?,'PENDING')", userId, PREFIX + "project");
        projectId = jdbc.queryForObject("SELECT id FROM pm_project WHERE name=?", Long.class, PREFIX + "project");
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @Test
    void taskAndInitialOutboxCommitTogetherAndRollbackTogether() {
        AnalysisTask committed = newTask("commit");
        submissionService.createTaskAndInitialEvent(committed);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pm_analysis_task WHERE id=?", Integer.class,
                committed.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pm_analysis_outbox WHERE task_id=?", Integer.class,
                committed.getId())).isEqualTo(1);

        String originalExchange = properties.getRabbit().getTopology().getMainExchange();
        properties.getRabbit().getTopology().setMainExchange("x".repeat(129));
        AnalysisTask rolledBack = newTask("rollback");
        try {
            assertThatThrownBy(() -> submissionService.createTaskAndInitialEvent(rolledBack))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            properties.getRabbit().getTopology().setMainExchange(originalExchange);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pm_analysis_task WHERE active_key=?", Integer.class,
                rolledBack.getActiveKey())).isZero();
    }

    @Test
    void concurrentRelaysAndConsumersClaimEachRecordOnlyOnce() throws Exception {
        properties.getRabbit().getOutbox().setBatchSize(3);
        for (int i = 0; i < 6; i++) {
            AnalysisTask task = newTask("relay-" + i);
            submissionService.createTaskAndInitialEvent(task);
        }

        List<List<AnalysisOutboxEvent>> claimed = runTogether(
                () -> claimService.claimBatch("relay-a"),
                () -> claimService.claimBatch("relay-b")
        );
        Set<Long> ids = new HashSet<>();
        claimed.forEach(batch -> batch.forEach(event -> ids.add(event.getId())));
        assertThat(ids).hasSize(6);
        assertThat(claimed.get(0)).hasSize(3);
        assertThat(claimed.get(1)).hasSize(3);

        AnalysisTask task = newTask("consumer-race");
        taskMapper.insert(task);
        List<Integer> results = runTogether(
                () -> claim(task, "worker-a", "message-a"),
                () -> claim(task, "worker-b", "message-b")
        );
        assertThat(results).containsExactlyInAnyOrder(0, 1);
        AnalysisTask claimedTask = taskMapper.selectById(task.getId());
        assertThat(claimedTask.getExecutionAttempt()).isEqualTo(1);
        assertThat(claimedTask.getExecutionVersion()).isEqualTo(1L);
    }

    @Test
    void onlyOneScannerRecoversExpiredLeaseAndOldFenceCannotComplete() throws Exception {
        AnalysisTask task = newTask("expired");
        task.setStatus("RUNNING");
        task.setWorkerId("old-worker");
        task.setLeaseExpiresAt(LocalDateTime.now().minusSeconds(30));
        task.setHeartbeatAt(LocalDateTime.now().minusSeconds(60));
        task.setExecutionAttempt(1);
        task.setExecutionVersion(7L);
        taskMapper.insert(task);

        List<AnalysisExecutionTransitionService.RecoveryResult> results = runTogether(
                transitions::recoverExpired,
                transitions::recoverExpired
        );
        assertThat(results.stream().mapToInt(result -> result.recovered().size()).sum()).isEqualTo(1);
        assertThat(taskMapper.selectById(task.getId()).getStatus()).isEqualTo("PENDING");

        assertThat(claim(task, "new-worker", "new-message")).isEqualTo(1);
        assertThat(taskMapper.completeSuccess(task.getId(), "old-worker", 7L, 999L)).isZero();
        assertThat(taskMapper.renewLease(task.getId(), "old-worker", 7L, 30)).isZero();
    }

    @Test
    void taskDebitRefundAndConcurrentBalanceUpdatesAreDatabaseIdempotent() throws Exception {
        List<Boolean> debits = runTogether(
                () -> creditService.consumeCreditsOnceForTask(userId, 1, "AI_AUDIT_REPORT", 10001L, "debit one"),
                () -> creditService.consumeCreditsOnceForTask(userId, 1, "AI_AUDIT_REPORT", 10002L, "debit two")
        );
        assertThat(debits).containsOnly(true);
        assertThat(balance()).isEqualTo(8);

        List<Boolean> duplicateDebit = runTogether(
                () -> creditService.consumeCreditsOnceForTask(userId, 1, "AI_AUDIT_REPORT", 10003L, "same debit"),
                () -> creditService.consumeCreditsOnceForTask(userId, 1, "AI_AUDIT_REPORT", 10003L, "same debit")
        );
        assertThat(duplicateDebit).containsExactlyInAnyOrder(true, false);
        assertThat(balance()).isEqualTo(7);

        List<Boolean> refunds = runTogether(
                () -> creditService.refundCreditsOnceForTask(userId, "AI_AUDIT_REPORT_REFUND", 10003L, "refund"),
                () -> creditService.refundCreditsOnceForTask(userId, "AI_AUDIT_REPORT_REFUND", 10003L, "refund")
        );
        assertThat(refunds).containsExactlyInAnyOrder(true, false);
        assertThat(balance()).isEqualTo(8);

        assertThat(creditService.refundCreditsOnceForTask(
                userId, "AI_AUDIT_REPORT_REFUND", 19999L, "no debit"
        )).isFalse();
        assertThat(balance()).isEqualTo(8);
    }

    @Test
    void firstPlanCreationRaceConvergesOnOneDatabaseRow() throws Exception {
        jdbc.update("DELETE FROM pm_user_plan WHERE user_id=?", userId);

        runTogether(
                () -> readCreditsAsUser(userId),
                () -> readCreditsAsUser(userId)
        );

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM pm_user_plan WHERE user_id=?", Integer.class, userId
        )).isEqualTo(1);
    }

    @Test
    void balanceAndCreditLogRollbackTogetherWhenLogInsertFails() {
        assertThatThrownBy(() -> creditService.consumeCreditsOnceForTask(
                userId, 1, "x".repeat(51), 18888L, "force log constraint failure"
        )).isInstanceOf(RuntimeException.class);

        assertThat(balance()).isEqualTo(10);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM pm_credit_log WHERE idempotency_key='ANALYSIS_DEBIT:18888'",
                Integer.class
        )).isZero();
    }

    @Test
    void fallbackPersistenceFailureRollsBackReportTaskProjectAndRefundTogether() {
        AnalysisTask task = newTask("fallback-rollback");
        task.setStatus("RUNNING");
        task.setWorkerId("worker-fallback");
        task.setLeaseExpiresAt(LocalDateTime.now().plusMinutes(2));
        task.setHeartbeatAt(LocalDateTime.now());
        task.setExecutionAttempt(4);
        task.setExecutionVersion(9L);
        taskMapper.insert(task);
        jdbc.update("UPDATE pm_project SET status='ANALYZING' WHERE id=?", projectId);
        creditService.consumeCreditsOnceForTask(
                userId, 1, "AI_AUDIT_REPORT", task.getId(), "fallback rollback debit"
        );

        jdbc.update("INSERT INTO pm_analysis_report(project_id,task_id,summary) VALUES (?,?,?)",
                projectId, task.getId(), "existing unique task report");
        AnalysisReport duplicate = new AnalysisReport();
        duplicate.setProjectId(projectId);
        duplicate.setTaskId(task.getId());
        duplicate.setSummary("must roll back");
        Project project = new Project();
        project.setId(projectId);
        project.setStatus("ANALYZING");
        AnalysisExecutionContext execution = new AnalysisExecutionContext(
                task.getId(), projectId, userId, "worker-fallback", 9L, 4,
                "message-fallback", task.getCorrelationId()
        );

        assertThatThrownBy(() -> reportPersistenceService.saveFallbackReportCompleteTaskAndRefund(
                duplicate, project, execution
        )).isInstanceOf(RuntimeException.class);

        AnalysisTask unchanged = taskMapper.selectById(task.getId());
        assertThat(unchanged.getStatus()).isEqualTo("RUNNING");
        assertThat(unchanged.getReportId()).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM pm_project WHERE id=?", String.class, projectId))
                .isEqualTo("ANALYZING");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM pm_analysis_report WHERE task_id=?", Integer.class, task.getId()
        )).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM pm_credit_log WHERE idempotency_key=?", Integer.class,
                "ANALYSIS_REFUND:" + task.getId()
        )).isZero();
        assertThat(balance()).isEqualTo(9);
    }

    private int claim(AnalysisTask task, String worker, String message) {
        return taskMapper.claimExecution(task.getId(), userId, projectId, task.getCorrelationId(),
                worker, message, 30, 4);
    }

    private int balance() {
        return jdbc.queryForObject("SELECT remaining_credits FROM pm_user_plan WHERE user_id=?", Integer.class, userId);
    }

    private Integer readCreditsAsUser(Long id) {
        try {
            UserContext.setUserId(id);
            return creditService.getMyCredits().getRemainingCredits();
        } finally {
            UserContext.clear();
        }
    }

    private AnalysisTask newTask(String suffix) {
        AnalysisTask task = new AnalysisTask();
        task.setUserId(userId);
        task.setProjectId(projectId);
        task.setTaskType("FULL_ANALYSIS");
        task.setActiveKey(PREFIX + suffix + ":" + UUID.randomUUID());
        task.setCorrelationId(PREFIX + UUID.randomUUID());
        task.setCreditCost(1);
        task.setStatus("PENDING");
        task.setProgress(0);
        return task;
    }

    private <T> List<T> runTogether(Callable<T> first, Callable<T> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<T> a = executor.submit(() -> awaitAndCall(ready, start, first));
            Future<T> b = executor.submit(() -> awaitAndCall(ready, start, second));
            ready.await();
            start.countDown();
            return List.of(a.get(), b.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private <T> T awaitAndCall(CountDownLatch ready, CountDownLatch start, Callable<T> callable) throws Exception {
        ready.countDown();
        start.await();
        return callable.call();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM pm_analysis_report WHERE task_id IN (SELECT id FROM pm_analysis_task WHERE active_key LIKE ?)", PREFIX + "%");
        jdbc.update("DELETE FROM pm_analysis_outbox WHERE task_id IN (SELECT id FROM pm_analysis_task WHERE active_key LIKE ?)", PREFIX + "%");
        jdbc.update("DELETE FROM pm_analysis_task WHERE active_key LIKE ? OR correlation_id LIKE ?", PREFIX + "%", PREFIX + "%");
        jdbc.update("DELETE FROM pm_credit_log WHERE user_id IN (SELECT id FROM pm_user WHERE username LIKE ?)", PREFIX + "%");
        jdbc.update("DELETE FROM pm_user_plan WHERE user_id IN (SELECT id FROM pm_user WHERE username LIKE ?)", PREFIX + "%");
        jdbc.update("DELETE FROM pm_project WHERE name LIKE ?", PREFIX + "%");
        jdbc.update("DELETE FROM pm_user WHERE username LIKE ?", PREFIX + "%");
    }
}

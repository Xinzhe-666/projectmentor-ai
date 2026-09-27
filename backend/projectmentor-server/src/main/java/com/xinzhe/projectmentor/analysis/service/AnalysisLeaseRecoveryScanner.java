package com.xinzhe.projectmentor.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "projectmentor.analysis", name = "dispatch-mode", havingValue = "rabbit")
public class AnalysisLeaseRecoveryScanner {

    private final AnalysisExecutionTransitionService transitions;
    private final AnalysisPipelineMetrics metrics;

    @Scheduled(
            fixedDelayString = "${projectmentor.analysis.rabbit.execution.recovery-scan-interval-millis:5000}",
            initialDelayString = "${projectmentor.analysis.rabbit.execution.recovery-scan-interval-millis:5000}"
    )
    public void recoverOnce() {
        try {
            AnalysisExecutionTransitionService.RecoveryResult result = transitions.recoverExpired();
            result.recovered().forEach(task -> metrics.lease("recovered"));
            for (int i = 0; i < result.failed().size(); i++) {
                metrics.lease("expired");
            }
        } catch (Exception e) {
            log.warn("analysis_recovery_scan_failed reason={}", SafePipelineError.from(e));
        }
    }
}

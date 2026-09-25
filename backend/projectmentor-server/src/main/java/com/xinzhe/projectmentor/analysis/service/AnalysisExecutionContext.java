package com.xinzhe.projectmentor.analysis.service;

public record AnalysisExecutionContext(
        Long taskId,
        Long projectId,
        Long userId,
        String workerId,
        Long executionVersion,
        Integer executionAttempt,
        String messageId,
        String correlationId
) {
}

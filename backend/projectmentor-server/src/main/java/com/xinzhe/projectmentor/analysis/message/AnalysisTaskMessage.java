package com.xinzhe.projectmentor.analysis.message;

import java.time.Instant;

public record AnalysisTaskMessage(
        String messageId,
        int schemaVersion,
        Long taskId,
        Long projectId,
        Long userId,
        int attempt,
        String correlationId,
        Instant createdAt
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public boolean hasRequiredFields() {
        return messageId != null && !messageId.isBlank()
                && taskId != null && projectId != null && userId != null
                && correlationId != null && !correlationId.isBlank()
                && createdAt != null;
    }
}

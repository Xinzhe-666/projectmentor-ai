package com.xinzhe.projectmentor.indexing.message;

public record VectorIndexMessage(String messageId, int schemaVersion, String eventType,
                                 long taskId, long userId, long projectId, long generation,
                                 String correlationId) {
}

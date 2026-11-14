package com.xinzhe.projectmentor.indexing.service;

public record VectorIndexExecutionContext(long taskId, long userId, long projectId, long generation,
                                          String workerId, long executionVersion, int executionAttempt) {
}

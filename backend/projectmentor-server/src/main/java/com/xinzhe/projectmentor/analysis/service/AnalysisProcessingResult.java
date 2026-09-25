package com.xinzhe.projectmentor.analysis.service;

public record AnalysisProcessingResult(Status status, String reason) {
    public enum Status {
        SUCCESS,
        RETRYABLE_FAILURE,
        NON_RETRYABLE_FAILURE,
        STALE
    }

    public static AnalysisProcessingResult success() {
        return new AnalysisProcessingResult(Status.SUCCESS, null);
    }
}

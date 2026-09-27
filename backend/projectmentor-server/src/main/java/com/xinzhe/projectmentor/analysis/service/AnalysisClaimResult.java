package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisTask;

public record AnalysisClaimResult(Decision decision, AnalysisExecutionContext execution, AnalysisTask task) {
    public enum Decision {
        CLAIMED,
        TERMINAL,
        DUPLICATE_RUNNING,
        EXHAUSTED,
        INVALID
    }
}

package com.xinzhe.projectmentor.analysis.service;

public class StaleAnalysisExecutionException extends RuntimeException {
    public StaleAnalysisExecutionException() {
        super("Analysis execution lease or fencing token is no longer valid");
    }
}

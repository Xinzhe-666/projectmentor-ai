package com.xinzhe.projectmentor.analysis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "projectmentor.analysis", name = "dispatch-mode", havingValue = "local", matchIfMissing = true)
public class LocalAnalysisTaskDispatcher implements AnalysisTaskDispatcher {

    private final AnalysisTaskAsyncExecutor asyncExecutor;

    @Override
    public void dispatch(Long taskId) {
        asyncExecutor.executeAnalysisTask(taskId);
    }
}

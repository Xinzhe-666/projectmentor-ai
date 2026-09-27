package com.xinzhe.projectmentor.analysis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "projectmentor.analysis", name = "dispatch-mode", havingValue = "rabbit")
public class RabbitAnalysisTaskDispatcher implements AnalysisTaskDispatcher {

    @Override
    public void dispatch(Long taskId) {
        // The initial event was committed atomically with the task. The relay owns network publication.
        log.debug("analysis_dispatch_enqueued taskId={}", taskId);
    }
}

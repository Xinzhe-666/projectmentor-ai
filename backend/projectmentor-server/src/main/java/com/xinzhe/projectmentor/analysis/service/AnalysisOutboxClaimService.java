package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisOutboxEvent;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisOutboxMapper;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisOutboxClaimService {

    private final AnalysisOutboxMapper outboxMapper;
    private final AnalysisPipelineProperties properties;

    @Transactional(rollbackFor = Exception.class)
    public List<AnalysisOutboxEvent> claimBatch(String claimOwner) {
        List<AnalysisOutboxEvent> candidates = outboxMapper.selectClaimableForUpdate(
                properties.getRabbit().getOutbox().getBatchSize()
        );
        List<AnalysisOutboxEvent> claimed = new ArrayList<>();
        for (AnalysisOutboxEvent event : candidates) {
            if (outboxMapper.claim(
                    event.getId(), claimOwner, properties.getRabbit().getOutbox().getClaimLeaseSeconds()
            ) == 1) {
                event.setClaimOwner(claimOwner);
                event.setStatus("CLAIMED");
                event.setPublishAttempt((event.getPublishAttempt() == null ? 0 : event.getPublishAttempt()) + 1);
                claimed.add(event);
            }
        }
        return claimed;
    }
}

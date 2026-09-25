package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisOutboxEvent;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisOutboxMapper;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisOutboxClaimServiceTests {

    @Test
    void claimIsAShortTransactionAndOnlyReturnsRowsConditionallyUpdatedByOwner() throws Exception {
        AnalysisOutboxMapper mapper = mock(AnalysisOutboxMapper.class);
        AnalysisPipelineProperties properties = new AnalysisPipelineProperties();
        properties.getRabbit().getOutbox().setBatchSize(10);
        properties.getRabbit().getOutbox().setClaimLeaseSeconds(30);
        AnalysisOutboxEvent first = event(1L, 0);
        AnalysisOutboxEvent lostRace = event(2L, 3);
        when(mapper.selectClaimableForUpdate(10)).thenReturn(List.of(first, lostRace));
        when(mapper.claim(1L, "relay-a", 30)).thenReturn(1);
        when(mapper.claim(2L, "relay-a", 30)).thenReturn(0);

        List<AnalysisOutboxEvent> claimed = new AnalysisOutboxClaimService(mapper, properties)
                .claimBatch("relay-a");

        assertThat(claimed).containsExactly(first);
        assertThat(first.getStatus()).isEqualTo("CLAIMED");
        assertThat(first.getClaimOwner()).isEqualTo("relay-a");
        assertThat(first.getPublishAttempt()).isEqualTo(1);
        verify(mapper).claim(2L, "relay-a", 30);

        Method method = AnalysisOutboxClaimService.class.getMethod("claimBatch", String.class);
        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }

    private AnalysisOutboxEvent event(Long id, int attempts) {
        AnalysisOutboxEvent event = new AnalysisOutboxEvent();
        event.setId(id);
        event.setStatus("NEW");
        event.setPublishAttempt(attempts);
        return event;
    }
}

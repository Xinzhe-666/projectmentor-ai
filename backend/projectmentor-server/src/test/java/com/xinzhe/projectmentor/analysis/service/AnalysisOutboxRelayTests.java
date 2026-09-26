package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.analysis.entity.AnalysisOutboxEvent;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisOutboxMapper;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisOutboxRelayTests {

    private AnalysisOutboxClaimService claimService;
    private AnalysisOutboxMapper mapper;
    private AnalysisRabbitPublisher publisher;
    private AnalysisExecutionTransitionService transitions;
    private AnalysisPipelineProperties properties;
    private AnalysisOutboxRelay relay;

    @BeforeEach
    void setUp() {
        claimService = mock(AnalysisOutboxClaimService.class);
        mapper = mock(AnalysisOutboxMapper.class);
        publisher = mock(AnalysisRabbitPublisher.class);
        transitions = mock(AnalysisExecutionTransitionService.class);
        AnalysisInstanceIdentity identity = mock(AnalysisInstanceIdentity.class);
        when(identity.instanceId()).thenReturn("instance-a");
        properties = new AnalysisPipelineProperties();
        relay = new AnalysisOutboxRelay(
                claimService, mapper, publisher, transitions, identity,
                properties, mock(AnalysisPipelineMetrics.class)
        );
    }

    @Test
    void onlyAckWithoutReturnMarksEventPublished() {
        AnalysisOutboxEvent event = event(1);
        when(claimService.claimBatch("instance-a:relay")).thenReturn(List.of(event));
        when(mapper.renewClaim(10L, "instance-a:relay", 30)).thenReturn(1);
        when(publisher.publish(event)).thenReturn(new RabbitPublishResult(RabbitPublishResult.Status.ACK, null));

        relay.relayOnce();

        verify(mapper).markPublished(10L, "instance-a:relay");
        verify(mapper, never()).reschedule(org.mockito.ArgumentMatchers.anyLong(), anyString(), anyInt(), anyString());
    }

    @Test
    void returnNackAndTimeoutNeverMarkPublished() {
        for (RabbitPublishResult.Status status : List.of(
                RabbitPublishResult.Status.RETURNED,
                RabbitPublishResult.Status.NACK,
                RabbitPublishResult.Status.TIMEOUT
        )) {
            AnalysisOutboxEvent event = event(1);
            when(claimService.claimBatch("instance-a:relay")).thenReturn(List.of(event));
            when(mapper.renewClaim(10L, "instance-a:relay", 30)).thenReturn(1);
            when(publisher.publish(event)).thenReturn(new RabbitPublishResult(status, "not reliable"));

            relay.relayOnce();
        }

        verify(mapper, never()).markPublished(org.mockito.ArgumentMatchers.anyLong(), anyString());
        verify(mapper, org.mockito.Mockito.times(3)).reschedule(
                org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq("instance-a:relay"),
                anyInt(), anyString()
        );
    }

    @Test
    void maximumPublishAttemptPersistsFailedStateAndTerminatesPendingTask() {
        properties.getRabbit().getOutbox().setMaximumAttempts(3);
        AnalysisOutboxEvent event = event(3);
        when(claimService.claimBatch("instance-a:relay")).thenReturn(List.of(event));
        when(mapper.renewClaim(10L, "instance-a:relay", 30)).thenReturn(1);
        when(publisher.publish(event)).thenReturn(new RabbitPublishResult(RabbitPublishResult.Status.NACK, "nack"));
        when(transitions.failClaimedOutboxAndPendingTask(
                10L, "instance-a:relay", 100L, "NACK: nack", "消息发布超过最大尝试次数"
        )).thenReturn(new com.xinzhe.projectmentor.analysis.entity.AnalysisTask());

        relay.relayOnce();

        verify(transitions).failClaimedOutboxAndPendingTask(
                10L, "instance-a:relay", 100L, "NACK: nack", "消息发布超过最大尝试次数"
        );
        verify(mapper, never()).markPublished(org.mockito.ArgumentMatchers.anyLong(), anyString());
    }

    @Test
    void staleOwnerCannotPublishAfterAnotherRelayTakesTheClaim() {
        AnalysisOutboxEvent event = event(1);
        when(claimService.claimBatch("instance-a:relay")).thenReturn(List.of(event));
        when(mapper.renewClaim(10L, "instance-a:relay", 30)).thenReturn(0);

        relay.relayOnce();

        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
        verify(mapper, never()).markPublished(org.mockito.ArgumentMatchers.anyLong(), anyString());
    }

    @Test
    void slowEarlierPublishDoesNotAuthorizeExpiredLaterClaim() {
        AnalysisOutboxEvent first = event(1);
        AnalysisOutboxEvent second = event(1);
        second.setId(11L);
        second.setEventId("event-11");
        when(claimService.claimBatch("instance-a:relay")).thenReturn(List.of(first, second));
        when(mapper.renewClaim(10L, "instance-a:relay", 30)).thenReturn(1);
        when(mapper.renewClaim(11L, "instance-a:relay", 30)).thenReturn(0);
        when(publisher.publish(first)).thenReturn(new RabbitPublishResult(RabbitPublishResult.Status.ACK, null));

        relay.relayOnce();

        verify(publisher).publish(first);
        verify(publisher, never()).publish(second);
        verify(mapper).markPublished(10L, "instance-a:relay");
        verify(mapper, never()).markPublished(11L, "instance-a:relay");
    }

    private AnalysisOutboxEvent event(int attempts) {
        AnalysisOutboxEvent event = new AnalysisOutboxEvent();
        event.setId(10L);
        event.setEventId("event-10");
        event.setTaskId(100L);
        event.setPublishAttempt(attempts);
        return event;
    }
}

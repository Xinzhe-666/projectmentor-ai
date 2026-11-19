package com.xinzhe.projectmentor.indexing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinzhe.projectmentor.analysis.service.AnalysisInstanceIdentity;
import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexOutboxEvent;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexOutboxMapper;
import com.xinzhe.projectmentor.indexing.message.VectorIndexMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class LocalVectorIndexDispatcherTests {
    @Test
    void scheduledRelayRecoversCommittedDeleteWithoutInMemoryEvent() throws Exception {
        VectorIndexWorker worker = mock(VectorIndexWorker.class);
        VectorIndexDeletionService deletions = mock(VectorIndexDeletionService.class);
        VectorIndexOutboxMapper outbox = mock(VectorIndexOutboxMapper.class);
        VectorIndexOutboxClaimService claims = mock(VectorIndexOutboxClaimService.class);
        VectorIndexTerminalTransitionService terminal = mock(VectorIndexTerminalTransitionService.class);
        AnalysisInstanceIdentity identity = mock(AnalysisInstanceIdentity.class);
        when(identity.instanceId()).thenReturn("instance-a");
        VectorIndexMessage message = new VectorIndexMessage("message-1", 1, "PROJECT_DELETE",
                77, 21, 31, 0, "correlation-1");
        ObjectMapper json = new ObjectMapper();
        VectorIndexOutboxEvent event = new VectorIndexOutboxEvent();
        event.setId(88L); event.setTaskId(77L); event.setEventId("message-1");
        event.setEventType("PROJECT_DELETE"); event.setPayload(json.writeValueAsString(message));
        event.setPublishAttempt(1);
        when(claims.claim("instance-a:local-index-relay")).thenReturn(List.of(event));
        when(deletions.process(message)).thenReturn(VectorIndexWorker.ProcessingResult.ACK);
        LocalVectorIndexDispatcher dispatcher = new LocalVectorIndexDispatcher(worker, deletions, outbox, claims,
                terminal, identity, json, new VectorIndexProperties());

        dispatcher.relay();

        verify(deletions).process(message);
        verify(outbox).markPublished(88, "instance-a:local-index-relay");
        verifyNoInteractions(worker, terminal);
    }
}

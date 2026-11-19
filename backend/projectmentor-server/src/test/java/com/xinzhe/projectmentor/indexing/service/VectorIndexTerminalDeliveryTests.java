package com.xinzhe.projectmentor.indexing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.xinzhe.projectmentor.analysis.service.AnalysisInstanceIdentity;
import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexOutboxEvent;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexTask;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexOutboxMapper;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexTaskMapper;
import com.xinzhe.projectmentor.indexing.message.VectorIndexMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VectorIndexTerminalDeliveryTests {
    @ParameterizedTest
    @CsvSource({"SUCCEEDED,false", "FAILED,false", "CANCELLED,false", "SUPERSEDED,false",
            "SUCCEEDED,true", "FAILED,true", "CANCELLED,true", "SUPERSEDED,true",
            "RUNNING,false", "RUNNING,true"})
    void failedClaimRoutesTerminalDuplicatesToAckAndKeepsRunningCompetition(String status, boolean cleanup)
            throws Exception {
        VectorIndexTaskMapper tasks = mock(VectorIndexTaskMapper.class);
        VectorIndexTask task = new VectorIndexTask();
        task.setStatus(status);
        when(tasks.selectById(77L)).thenReturn(task);
        // claim returns zero: this delivery never owns the execution.
        AnalysisInstanceIdentity identity = mock(AnalysisInstanceIdentity.class);
        when(identity.newWorkerId()).thenReturn("late-worker");
        when(identity.instanceId()).thenReturn("local");
        VectorIndexProperties properties = new VectorIndexProperties();
        VectorIndexWorker worker = new VectorIndexWorker(tasks, null, null, null, null, null,
                null, null, null, properties, identity, null, null);
        VectorIndexDeletionService deletion = new VectorIndexDeletionService(null, tasks, null, null,
                null, null, null, identity, properties, null, null);
        VectorIndexMessage message = new VectorIndexMessage("late-message", 1,
                cleanup ? "PROJECT_DELETE" : "INDEX_EXECUTE", 77, 21, 31, cleanup ? 0 : 4, "correlation");
        boolean running = "RUNNING".equals(status);
        assertEquals(running ? VectorIndexWorker.ProcessingResult.RETRY_LATER : VectorIndexWorker.ProcessingResult.ACK,
                cleanup ? deletion.process(message) : worker.process(message));

        ObjectMapper json = new ObjectMapper();
        VectorIndexRetryService retries = mock(VectorIndexRetryService.class);
        when(retries.schedule(message)).thenReturn(true);
        RabbitVectorIndexConsumer consumer = new RabbitVectorIndexConsumer(json, worker, deletion, retries);
        MessageProperties headers = new MessageProperties();
        headers.setDeliveryTag(9);
        Channel channel = mock(Channel.class);
        consumer.consume(new Message(json.writeValueAsBytes(message), headers), channel);
        verify(channel).basicAck(9, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
        if (running) verify(retries).schedule(message);
        else verifyNoInteractions(retries); // No retry service call means no Retry Outbox can be created.

        VectorIndexOutboxMapper outbox = mock(VectorIndexOutboxMapper.class);
        VectorIndexOutboxClaimService claims = mock(VectorIndexOutboxClaimService.class);
        VectorIndexTerminalTransitionService terminal = mock(VectorIndexTerminalTransitionService.class);
        VectorIndexOutboxEvent event = new VectorIndexOutboxEvent();
        event.setId(88L); event.setTaskId(77L); event.setPublishAttempt(1);
        event.setPayload(json.writeValueAsString(message));
        when(claims.claim("local:local-index-relay")).thenReturn(List.of(event));
        new LocalVectorIndexDispatcher(worker, deletion, outbox, claims, terminal, identity, json, properties).relay();
        if (running) {
            verify(outbox).reschedule(eq(88L), eq("local:local-index-relay"), anyInt(), anyString());
            verify(outbox, never()).markPublished(anyLong(), anyString());
        } else {
            verify(outbox).markPublished(88, "local:local-index-relay");
            verify(outbox, never()).reschedule(anyLong(), anyString(), anyInt(), anyString());
        }
        verifyNoInteractions(terminal);
        verify(tasks, never()).releaseForRetry(anyLong(), anyString(), anyLong(), anyString(), anyString());
    }
}

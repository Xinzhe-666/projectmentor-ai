package com.xinzhe.projectmentor.integration;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.xinzhe.projectmentor.analysis.entity.AnalysisOutboxEvent;
import com.xinzhe.projectmentor.analysis.mapper.AnalysisOutboxMapper;
import com.xinzhe.projectmentor.analysis.service.AnalysisOutboxRelay;
import com.xinzhe.projectmentor.analysis.service.AnalysisOutboxClaimService;
import com.xinzhe.projectmentor.analysis.service.AnalysisRabbitPublisher;
import com.xinzhe.projectmentor.analysis.service.AnalysisExecutionTransitionService;
import com.xinzhe.projectmentor.analysis.service.AnalysisInstanceIdentity;
import com.xinzhe.projectmentor.analysis.service.AnalysisPipelineMetrics;
import com.xinzhe.projectmentor.analysis.service.RabbitPublishResult;
import com.xinzhe.projectmentor.config.AnalysisPipelineProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "projectmentor.analysis.dispatch-mode=rabbit",
        "projectmentor.analysis.rabbit.outbox.poll-interval-millis=3600000",
        "projectmentor.analysis.rabbit.execution.recovery-scan-interval-millis=3600000",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "projectmentor.ai.enabled=false"
})
class AnalysisPipelineRabbitIT {

    private static final String PREFIX = "pmai.it.";

    @Autowired private RabbitAdmin admin;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private ConnectionFactory connectionFactory;
    @Autowired private AnalysisPipelineProperties properties;
    @Autowired private AnalysisOutboxMapper outboxMapper;
    @Autowired private AnalysisOutboxRelay relay;
    @Autowired private AnalysisOutboxClaimService claimService;
    @Autowired private AnalysisExecutionTransitionService transitions;
    @Autowired private AnalysisInstanceIdentity identity;
    @Autowired private AnalysisPipelineMetrics metrics;
    @Autowired private JdbcTemplate jdbc;

    private String suffix;
    private String mainExchange;
    private String mainQueue;
    private String route;
    private String deadExchange;
    private String deadQueue;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        mainExchange = PREFIX + "main." + suffix;
        mainQueue = PREFIX + "queue." + suffix;
        route = PREFIX + "route." + suffix;
        deadExchange = PREFIX + "dlx." + suffix;
        deadQueue = PREFIX + "dead." + suffix;

        DirectExchange main = new DirectExchange(mainExchange, true, false);
        DirectExchange dead = new DirectExchange(deadExchange, true, false);
        Queue queue = QueueBuilder.durable(mainQueue)
                .quorum()
                .deadLetterExchange(deadExchange)
                .deadLetterRoutingKey(route)
                .build();
        Queue dlq = QueueBuilder.durable(deadQueue).quorum().build();
        admin.declareExchange(main);
        admin.declareExchange(dead);
        admin.declareQueue(queue);
        admin.declareQueue(dlq);
        admin.declareBinding(BindingBuilder.bind(queue).to(main).with(route));
        admin.declareBinding(BindingBuilder.bind(dlq).to(dead).with(route));
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM pm_analysis_outbox WHERE event_id LIKE ?", PREFIX + "%");
        admin.deleteQueue(mainQueue);
        admin.deleteQueue(deadQueue);
        admin.deleteExchange(mainExchange);
        admin.deleteExchange(deadExchange);
        admin.deleteExchange(PREFIX + "retry." + suffix);
        admin.deleteExchange(PREFIX + "missing." + suffix);
    }

    @Test
    void confirmAckRequiresRoutabilityAndUsesPersistentMessages() {
        AnalysisRabbitPublisher publisher = new AnalysisRabbitPublisher(rabbitTemplate, properties);
        AnalysisOutboxEvent routed = event(mainExchange, route);

        assertThat(publisher.publish(routed).status()).isEqualTo(RabbitPublishResult.Status.ACK);
        Message received = rabbitTemplate.receive(mainQueue, 3000);
        assertThat(received).isNotNull();
        assertThat(received.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);

        AnalysisOutboxEvent unroutable = event(mainExchange, "missing.binding");
        assertThat(publisher.publish(unroutable).status()).isEqualTo(RabbitPublishResult.Status.RETURNED);
    }

    @Test
    void unackedDeliveryIsRedeliveredAndCanBeManuallyAcked() throws Exception {
        rabbitTemplate.send(mainExchange, route, persistentMessage("redeliver-me"));
        Connection connection = connectionFactory.createConnection();
        try {
            Channel firstChannel = connection.createChannel(false);
            GetResponse first = awaitGet(firstChannel, mainQueue, false);
            assertThat(first).isNotNull();
            firstChannel.close();

            Channel secondChannel = connection.createChannel(false);
            GetResponse redelivered = awaitGet(secondChannel, mainQueue, false);
            assertThat(redelivered.getEnvelope().isRedeliver()).isTrue();
            secondChannel.basicAck(redelivered.getEnvelope().getDeliveryTag(), false);
            secondChannel.close();
        } finally {
            connection.close();
        }
        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> assertThat(admin.getQueueInfo(mainQueue).getMessageCount()).isZero()
        );
    }

    @Test
    void retryQueueReturnsAfterTtlAndRejectedMessageReachesDlq() throws Exception {
        String retryExchange = PREFIX + "retry." + suffix;
        String retryQueue = mainQueue + ".retry";
        DirectExchange retry = new DirectExchange(retryExchange, true, false);
        Queue delayed = QueueBuilder.durable(retryQueue)
                .quorum()
                .ttl(250)
                .deadLetterExchange(mainExchange)
                .deadLetterRoutingKey(route)
                .build();
        admin.declareExchange(retry);
        admin.declareQueue(delayed);
        admin.declareBinding(BindingBuilder.bind(delayed).to(retry).with(route));
        try {
            rabbitTemplate.send(retryExchange, route, persistentMessage("retry-once"));
            await().atMost(Duration.ofSeconds(5)).untilAsserted(
                    () -> assertThat(rabbitTemplate.receive(mainQueue)).isNotNull()
            );

            rabbitTemplate.send(mainExchange, route, persistentMessage("terminal-diagnostic"));
            Connection connection = connectionFactory.createConnection();
            try {
                Channel channel = connection.createChannel(false);
                GetResponse delivery = awaitGet(channel, mainQueue, false);
                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
                channel.close();
            } finally {
                connection.close();
            }
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                Message diagnostic = rabbitTemplate.receive(deadQueue);
                assertThat(diagnostic).isNotNull();
                assertThat(new String(diagnostic.getBody(), StandardCharsets.UTF_8))
                        .isEqualTo("terminal-diagnostic");
            });
        } finally {
            admin.deleteQueue(retryQueue);
        }
    }

    @Test
    void outboxIsRetainedWhenBrokerIsUnavailableAndPublishesAfterRecovery() {
        AnalysisOutboxEvent event = event(mainExchange, route);
        event.setTaskId(900000L);
        outboxMapper.insert(event);

        org.springframework.amqp.rabbit.connection.CachingConnectionFactory unavailable =
                new org.springframework.amqp.rabbit.connection.CachingConnectionFactory("127.0.0.1", 1);
        unavailable.setPublisherConfirmType(
                org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType.CORRELATED
        );
        unavailable.setPublisherReturns(true);
        unavailable.getRabbitConnectionFactory().setConnectionTimeout(500);
        RabbitTemplate unavailableTemplate = new RabbitTemplate(unavailable);
        unavailableTemplate.setMandatory(true);
        try {
            AnalysisOutboxRelay offlineRelay = new AnalysisOutboxRelay(
                    claimService,
                    outboxMapper,
                    new AnalysisRabbitPublisher(unavailableTemplate, properties),
                    transitions,
                    identity,
                    properties,
                    metrics
            );
            offlineRelay.relayOnce();
            AnalysisOutboxEvent retained = outboxMapper.selectById(event.getId());
            assertThat(retained.getStatus()).isEqualTo("NEW");
            assertThat(retained.getLastError()).isNotBlank();
        } finally {
            unavailable.destroy();
        }

        jdbc.update("UPDATE pm_analysis_outbox SET next_attempt_at=NOW(6) WHERE id=?", event.getId());

        relay.relayOnce();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(outboxMapper.selectById(event.getId()).getStatus()).isEqualTo("PUBLISHED")
        );
        assertThat(rabbitTemplate.receive(mainQueue, 3000)).isNotNull();
    }

    private AnalysisOutboxEvent event(String exchange, String routingKey) {
        AnalysisOutboxEvent event = new AnalysisOutboxEvent();
        event.setEventId(PREFIX + UUID.randomUUID());
        event.setTaskId(1L);
        event.setEventType("ANALYSIS_EXECUTE");
        event.setSchemaVersion(1);
        event.setPayload("{\"messageId\":\"" + event.getEventId() + "\",\"schemaVersion\":1,\"taskId\":1}");
        event.setExchangeName(exchange);
        event.setRoutingKey(routingKey);
        event.setStatus("NEW");
        event.setPublishAttempt(0);
        event.setNextAttemptAt(LocalDateTime.now());
        return event;
    }

    private Message persistentMessage(String body) {
        return MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
    }

    private GetResponse awaitGet(Channel channel, String queue, boolean autoAck) {
        AtomicReference<GetResponse> response = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            GetResponse current = channel.basicGet(queue, autoAck);
            assertThat(current).isNotNull();
            response.set(current);
        });
        return response.get();
    }
}

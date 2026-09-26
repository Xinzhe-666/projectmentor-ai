package com.xinzhe.projectmentor.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "projectmentor.analysis", name = "dispatch-mode", havingValue = "rabbit")
public class RabbitAnalysisConfig {

    @Bean
    public Declarables analysisRabbitTopology(AnalysisPipelineProperties properties) {
        AnalysisPipelineProperties.Topology topology = properties.getRabbit().getTopology();
        requireQuorum(topology);
        int deliveryLimit = properties.getRabbit().getConsumer().getDeliveryLimit();
        DirectExchange mainExchange = new DirectExchange(topology.getMainExchange(), true, false);
        DirectExchange retryExchange = new DirectExchange(topology.getRetryExchange(), true, false);
        DirectExchange deadExchange = new DirectExchange(topology.getDeadLetterExchange(), true, false);

        Queue mainQueue = deadLetteringQuorumQueue(topology.getMainQueue(), deliveryLimit)
                .deadLetterExchange(topology.getDeadLetterExchange())
                .deadLetterRoutingKey(topology.getDeadLetterRoutingKey())
                .build();
        Queue deadQueue = QueueBuilder.durable(topology.getDeadLetterQueue()).quorum().build();

        List<Declarable> declarables = new ArrayList<>();
        declarables.add(mainExchange);
        declarables.add(retryExchange);
        declarables.add(deadExchange);
        declarables.add(mainQueue);
        declarables.add(deadQueue);
        declarables.add(BindingBuilder.bind(mainQueue).to(mainExchange).with(topology.getMainRoutingKey()));
        declarables.add(BindingBuilder.bind(deadQueue).to(deadExchange).with(topology.getDeadLetterRoutingKey()));

        for (Long ttlSeconds : properties.getRabbit().getRetry().getTtlSeconds()) {
            String retryQueueName = retryQueueName(topology, ttlSeconds);
            String retryRoutingKey = retryRoutingKey(ttlSeconds);
            Queue retryQueue = deadLetteringQuorumQueue(retryQueueName, deliveryLimit)
                    .ttl(Math.toIntExact(ttlSeconds * 1000))
                    .deadLetterExchange(topology.getMainExchange())
                    .deadLetterRoutingKey(topology.getMainRoutingKey())
                    .build();
            Binding retryBinding = BindingBuilder.bind(retryQueue).to(retryExchange).with(retryRoutingKey);
            declarables.add(retryQueue);
            declarables.add(retryBinding);
        }
        return new Declarables(declarables);
    }

    @Bean("analysisRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory analysisRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            AnalysisPipelineProperties properties
    ) {
        AnalysisPipelineProperties.Consumer consumer = properties.getRabbit().getConsumer();
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(consumer.getMinConcurrency());
        factory.setMaxConcurrentConsumers(consumer.getMaxConcurrency());
        factory.setPrefetchCount(consumer.getPrefetch());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    public static String retryQueueName(AnalysisPipelineProperties.Topology topology, long ttlSeconds) {
        return topology.getMainQueue() + ".retry." + ttlSeconds + "s";
    }

    public static String retryRoutingKey(long ttlSeconds) {
        return "analysis.retry." + ttlSeconds + "s.v1";
    }

    private QueueBuilder deadLetteringQuorumQueue(String name, int deliveryLimit) {
        return QueueBuilder.durable(name)
                .quorum()
                .withArgument("x-dead-letter-strategy", "at-least-once")
                .withArgument("x-overflow", "reject-publish")
                .withArgument("x-delivery-limit", deliveryLimit);
    }

    private void requireQuorum(AnalysisPipelineProperties.Topology topology) {
        if (topology.getQueueType() != AnalysisPipelineProperties.QueueType.QUORUM) {
            throw new IllegalStateException("Rabbit analysis pipeline requires quorum queues");
        }
    }
}

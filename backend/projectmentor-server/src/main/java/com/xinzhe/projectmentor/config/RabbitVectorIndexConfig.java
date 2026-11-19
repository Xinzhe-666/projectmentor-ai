package com.xinzhe.projectmentor.config;

import org.springframework.amqp.core.AcknowledgeMode;
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
@ConditionalOnProperty(prefix = "projectmentor.vector-index", name = "dispatch-mode", havingValue = "rabbit")
public class RabbitVectorIndexConfig {
    @Bean
    Declarables vectorIndexRabbitTopology(VectorIndexProperties p) {
        DirectExchange main = new DirectExchange(p.getMainExchange(), true, false);
        DirectExchange retry = new DirectExchange(p.getRetryExchange(), true, false);
        DirectExchange dead = new DirectExchange(p.getDeadLetterExchange(), true, false);
        Queue mainQueue = quorum(p.getMainQueue()).deadLetterExchange(p.getDeadLetterExchange())
                .deadLetterRoutingKey(p.getDeadLetterRoutingKey()).build();
        Queue deadQueue = QueueBuilder.durable(p.getDeadLetterQueue()).quorum().build();
        List<Declarable> declarations = new ArrayList<>(List.of(main, retry, dead, mainQueue, deadQueue,
                BindingBuilder.bind(mainQueue).to(main).with(p.getMainRoutingKey()),
                BindingBuilder.bind(deadQueue).to(dead).with(p.getDeadLetterRoutingKey())));
        for (Long seconds : p.getRetryTtlSeconds()) {
            Queue retryQueue = quorum(retryQueueName(p, seconds)).ttl(Math.toIntExact(seconds * 1000L))
                    .deadLetterExchange(p.getMainExchange()).deadLetterRoutingKey(p.getMainRoutingKey()).build();
            declarations.add(retryQueue);
            declarations.add(BindingBuilder.bind(retryQueue).to(retry).with(retryRoutingKey(seconds)));
        }
        return new Declarables(declarations);
    }

    @Bean("vectorIndexRabbitListenerContainerFactory")
    SimpleRabbitListenerContainerFactory vectorIndexRabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                                     VectorIndexProperties p) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(p.getConsumerConcurrency());
        factory.setMaxConcurrentConsumers(p.getConsumerConcurrency());
        factory.setPrefetchCount(p.getPrefetch());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    private QueueBuilder quorum(String name) {
        return QueueBuilder.durable(name).quorum().withArgument("x-dead-letter-strategy", "at-least-once")
                .withArgument("x-overflow", "reject-publish").withArgument("x-delivery-limit", 20);
    }
    public static String retryQueueName(VectorIndexProperties p, long seconds) {
        return p.getMainQueue() + ".retry." + seconds + "s";
    }
    public static String retryRoutingKey(long seconds) { return "index.retry." + seconds + "s.v1"; }
}

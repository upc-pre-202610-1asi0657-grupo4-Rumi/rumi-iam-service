package com.rumi.iam.infrastructure.messaging.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Queue of IAM on the Building Management exchange. A message that cannot be applied goes to the dead letter
 * queue instead of being redelivered forever.
 */
@Configuration
public class IamMessagingConfiguration {

    public static final String BUILDING_MANAGEMENT_EXCHANGE = "rumi.building-management.events";
    public static final String BUILDING_REGISTERED_ROUTING_KEY = "building-management.building.registered";
    public static final String BUILDING_REGISTERED_QUEUE = "rumi.iam.building-registered";
    public static final String BUILDING_REGISTERED_DLQ = BUILDING_REGISTERED_QUEUE + ".dlq";

    @Bean
    public TopicExchange buildingManagementEventsExchange() {
        return new TopicExchange(BUILDING_MANAGEMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue buildingRegisteredQueue() {
        return QueueBuilder.durable(BUILDING_REGISTERED_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(BUILDING_REGISTERED_DLQ)
                .build();
    }

    @Bean
    public Queue buildingRegisteredDeadLetterQueue() {
        return QueueBuilder.durable(BUILDING_REGISTERED_DLQ).build();
    }

    @Bean
    public Binding buildingRegisteredBinding(Queue buildingRegisteredQueue, TopicExchange buildingManagementEventsExchange) {
        return BindingBuilder.bind(buildingRegisteredQueue)
                .to(buildingManagementEventsExchange)
                .with(BUILDING_REGISTERED_ROUTING_KEY);
    }
}

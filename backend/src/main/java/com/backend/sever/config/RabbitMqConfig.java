package com.backend.sever.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    private final String couponClaimExchange;
    private final String couponClaimQueue;
    private final String couponClaimRoutingKey;

    public RabbitMqConfig(
            @Value("${app.rabbitmq.coupon-claim-exchange}") String couponClaimExchange,
            @Value("${app.rabbitmq.coupon-claim-queue}") String couponClaimQueue,
            @Value("${app.rabbitmq.coupon-claim-routing-key}") String couponClaimRoutingKey
    ) {
        this.couponClaimExchange = couponClaimExchange;
        this.couponClaimQueue = couponClaimQueue;
        this.couponClaimRoutingKey = couponClaimRoutingKey;
    }

    @Bean
    public DirectExchange couponClaimExchange() {
        return new DirectExchange(couponClaimExchange, true, false);
    }

    @Bean
    public Queue couponClaimQueue() {
        return QueueBuilder.durable(couponClaimQueue).build();
    }

    @Bean
    public Binding couponClaimBinding(Queue couponClaimQueue, DirectExchange couponClaimExchange) {
        return BindingBuilder.bind(couponClaimQueue).to(couponClaimExchange).with(couponClaimRoutingKey);
    }
}

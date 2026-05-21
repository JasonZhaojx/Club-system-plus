package com.backend.sever.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CouponClaimMessageProducer {
    private static final Logger log = LoggerFactory.getLogger(CouponClaimMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public CouponClaimMessageProducer(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.coupon-claim-exchange}") String exchange,
            @Value("${app.rabbitmq.coupon-claim-routing-key}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void sendAfterCommit(Long taskId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(taskId);
                }
            });
            return;
        }
        send(taskId);
    }

    public void send(Long taskId) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, String.valueOf(taskId));
        } catch (RuntimeException ex) {
            log.warn("Failed to publish coupon claim task {}, compensation scheduler will retry", taskId, ex);
        }
    }
}

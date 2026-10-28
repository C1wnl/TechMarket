package com.marketplace.order.payment;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "payment.integration.enabled", havingValue = "true")
public class PaymentEventConsumer {
    private final PaymentInboxService inbox;
    public PaymentEventConsumer(PaymentInboxService inbox) { this.inbox = inbox; }
    @KafkaListener(topics = "payment-events", groupId = "${payment.kafka.group-id:order-service-group}",
                   autoStartup = "${payment.kafka.auto-startup:true}")
    public void consume(ConsumerRecord<String, String> record) {
        // El offset se confirma sólo después del commit de inbox; no hay HTTP en este hilo.
        inbox.receive(record.value(), record.topic() + ":" + record.partition() + ":" + record.offset());
    }
}

package com.marketplace.order.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "payment.integration.enabled", havingValue = "true")
public class PaymentIntegrationConfig {
    @Bean
    public DefaultErrorHandler paymentKafkaErrorHandler() {
        var handler = new DefaultErrorHandler(new FixedBackOff(2000L, FixedBackOff.UNLIMITED_ATTEMPTS));
        // Un fallo al guardar inbox nunca debe terminar en un descarte por recuperación por defecto.
        handler.setClassifications(java.util.Map.of(Exception.class, true), true);
        return handler;
    }
}

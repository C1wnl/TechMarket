
package com.marketplace.order.payment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {

        // Reintenta indefinidamente, esperando 1 segundo
        // entre cada intento.
        FixedBackOff backOff = new FixedBackOff(
                1000L,
                FixedBackOff.UNLIMITED_ATTEMPTS
        );

        return new DefaultErrorHandler(backOff);
    }
}


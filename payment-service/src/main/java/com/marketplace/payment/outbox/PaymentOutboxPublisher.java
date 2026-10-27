
package com.marketplace.payment.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class PaymentOutboxPublisher {

    private static final Logger logger =
            LoggerFactory.getLogger(PaymentOutboxPublisher.class);

    private static final String TOPIC = "payment-events";

    private final PaymentOutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentOutboxPublisher(
            PaymentOutboxRepository repository,
            KafkaTemplate<String, String> kafkaTemplate) {

        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 5000)
    public void publicarPendientes() {

        List<PaymentOutboxEvent> pendientes =
                repository.findTop20ByEnviadoFalseOrderByIdAsc();

        for (PaymentOutboxEvent evento : pendientes) {

            try {
                kafkaTemplate.send(
                        TOPIC,
                        evento.getPaymentId().toString(),
                        evento.getPayload()
                ).get(10, TimeUnit.SECONDS);

                evento.setEnviado(true);
                repository.save(evento);

                logger.info(
                        "Outbox {} enviado a Kafka. Evento: {}",
                        evento.getId(),
                        evento.getEventId()
                );

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                logger.warn(
                        "Publicación interrumpida. Outbox pendiente: {}",
                        evento.getId()
                );

                return;

            } catch (Exception e) {

                logger.error(
                        "No se pudo publicar el Outbox {}. "
                                + "Se reintentará posteriormente.",
                        evento.getId(),
                        e
                );

                // Conservamos este evento y los siguientes
                // pendientes para el próximo intento.
                return;
            }
        }
    }
}

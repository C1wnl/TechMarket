
package com.marketplace.notification.kafka;

import com.marketplace.notification.event.PaymentEvent;
import com.marketplace.notification.model.Notification;
import com.marketplace.notification.model.ProcessedPaymentEvent;
import com.marketplace.notification.repository.ProcessedPaymentEventRepository;
import com.marketplace.notification.service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

@Component
public class PaymentEventConsumer {

    private static final Logger logger =
            LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final JsonMapper jsonMapper;
    private final NotificationService notificationService;
    private final ProcessedPaymentEventRepository processedRepository;

    public PaymentEventConsumer(
            JsonMapper jsonMapper,
            NotificationService notificationService,
            ProcessedPaymentEventRepository processedRepository) {

        this.jsonMapper = jsonMapper;
        this.notificationService = notificationService;
        this.processedRepository = processedRepository;
    }

    @Transactional
    @KafkaListener(
            topics = "payment-events",
            groupId = "notification-service-group"
    )
    public void consumir(String mensaje) {

        PaymentEvent event =
                jsonMapper.readValue(mensaje, PaymentEvent.class);

        if (event.eventId() == null || event.eventId().isBlank()
                || event.userId() == null || event.userId() <= 0
                || event.paymentId() == null || event.monto() == null) {
            throw new IllegalArgumentException(
                    "Evento de pago incompleto"
            );
        }

        String tipo;
        String texto;

        if ("PAYMENT_APPROVED".equals(event.eventType())
                && "APROBADO".equals(event.estado())) {

            tipo = "PAGO_APROBADO";
            texto = "Tu pago de " + event.monto()
                    + " fue aprobado correctamente.";

        } else if ("PAYMENT_REJECTED".equals(event.eventType())
                && "RECHAZADO".equals(event.estado())) {

            tipo = "PAGO_RECHAZADO";
            texto = "Tu pago de " + event.monto()
                    + " fue rechazado.";

        } else {
            throw new IllegalArgumentException(
                    "Tipo o estado de evento no reconocido"
            );
        }

        if (processedRepository.existsById(event.eventId())) {
            logger.info(
                    "Evento duplicado ignorado: {}",
                    event.eventId()
            );
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(event.userId());
        notification.setTipo(tipo);
        notification.setMensaje(texto);

        notificationService.crear(notification);

        processedRepository.save(
                new ProcessedPaymentEvent(event.eventId())
        );

        logger.info(
                "Evento {} procesado para usuario {}",
                event.eventId(),
                event.userId()
        );
    }
}


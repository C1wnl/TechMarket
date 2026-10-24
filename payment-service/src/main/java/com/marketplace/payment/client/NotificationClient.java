
package com.marketplace.payment.client;

import com.marketplace.payment.dto.NotificationRequest;
import com.marketplace.payment.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NotificationClient {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;

    public NotificationClient(
            @Value("${notification.service.url:http://localhost:8086}")
            String notificationUrl) {

        this.restClient = RestClient.builder()
                .baseUrl(notificationUrl)
                .build();
    }

    public void notificarCambioDePago(Payment payment) {

        String tipo;
        String mensaje;

        if ("APROBADO".equals(payment.getEstado())) {
            tipo = "PAGO_APROBADO";
            mensaje = "Tu pago de " + payment.getMonto()
                    + " fue aprobado correctamente.";

        } else if ("RECHAZADO".equals(payment.getEstado())) {
            tipo = "PAGO_RECHAZADO";
            mensaje = "Tu pago de " + payment.getMonto()
                    + " fue rechazado.";

        } else {
            return;
        }

        NotificationRequest request = new NotificationRequest(
                payment.getUserId(),
                tipo,
                mensaje
        );

        try {
            restClient.post()
                    .uri("/notifications")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Notificación enviada para el pago {}",
                    payment.getId());

        } catch (RestClientException e) {
            log.error(
                    "No se pudo notificar el cambio del pago {}",
                    payment.getId(),
                    e
            );
        }
    }
}

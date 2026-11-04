package com.marketplace.order.client;

import com.marketplace.order.model.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@ConditionalOnProperty(
        name = "payment.integration.enabled",
        havingValue = "true"
)
public class PaymentClient {

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentResponse(
            Long id,
            Long orderId,
            Long userId,
            BigDecimal monto,
            String metodoPago,
            String estado
    ) {}

    private final RestClient client;
    private final String method;

    public PaymentClient(
            @Value("${payment.service.url:http://localhost:8085}") String url,
            @Value("${payment.service.connect-timeout-ms:2000}") long connectTimeout,
            @Value("${payment.service.read-timeout-ms:3000}") long readTimeout,
            @Value("${payment.service.metodo:TARJETA}") String method,
            @Value("${payment.internal.key}") String internalKey
    ) {

        if (internalKey == null || internalKey.isBlank()) {
            throw new IllegalStateException(
                    "PAYMENT_INTERNAL_KEY no está configurada"
            );
        }

        if (method == null || method.isBlank()) {
            throw new IllegalStateException(
                    "El método de pago no está configurado"
            );
        }

        if (connectTimeout <= 0 || readTimeout <= 0) {
            throw new IllegalArgumentException(
                    "Los tiempos de espera deben ser mayores que cero"
            );
        }

        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(connectTimeout))
                        .build()
        );

        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        client = RestClient.builder()
                .baseUrl(url)
                .defaultHeader("X-Internal-Key", internalKey)
                .requestFactory(factory)
                .build();

        this.method = method;
    }

    public PaymentResponse crear(Order order) {

        if (order == null
                || order.getId() == null
                || order.getUserId() == null
                || order.getTotal() == null
                || order.getTotal().compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "El pedido contiene datos inválidos para crear el pago"
            );
        }

        var payment = client.post()
                .uri("/payments")
                .body(Map.of(
                        "orderId", order.getId(),
                        "userId", order.getUserId(),
                        "monto", order.getTotal(),
                        "metodoPago", method
                ))
                .retrieve()
                .body(PaymentResponse.class);

        if (payment == null
                || payment.id() == null
                || payment.id() <= 0
                || !Objects.equals(payment.orderId(), order.getId())
                || !Objects.equals(payment.userId(), order.getUserId())
                || payment.monto() == null
                || payment.monto().compareTo(order.getTotal()) != 0
                || !Objects.equals(method, payment.metodoPago())
                || payment.estado() == null
                || !Set.of("PENDIENTE", "APROBADO", "RECHAZADO", "CANCELADO")
                .contains(payment.estado())) {

            throw new IllegalStateException(
                    "Payment devolvió un pago incompatible; revisar antes de recuperar"
            );
        }

        return payment;
    }
}

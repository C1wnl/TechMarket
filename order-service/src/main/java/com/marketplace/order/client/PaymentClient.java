package com.marketplace.order.client;

import com.marketplace.order.model.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "payment.integration.enabled", havingValue = "true")
public class PaymentClient {
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentResponse(Long id, Long orderId, Long userId, BigDecimal monto,
                                  String metodoPago, String estado) {}
    private final RestClient client;
    private final String method;
    public PaymentClient(@Value("${payment.service.url:http://localhost:8085}") String url,
            @Value("${payment.service.connect-timeout-ms:2000}") long connectTimeout,
            @Value("${payment.service.read-timeout-ms:3000}") long readTimeout,
            @Value("${payment.service.metodo:TARJETA}") String method) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout)).build());
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
        this.method = method;
    }
    public PaymentResponse crear(Order order) {
        var payment = client.post().uri("/payments")
                .body(Map.of("orderId", order.getId(), "userId", order.getUserId(),
                        "monto", order.getTotal(), "metodoPago", method))
                .retrieve().body(PaymentResponse.class);
        // Una repetición puede devolver el pago ya finalizado. Kafka conserva el resultado.
        if (payment == null || payment.id() == null || payment.id() <= 0
                || !Objects.equals(payment.orderId(), order.getId())
                || !Objects.equals(payment.userId(), order.getUserId())
                || payment.monto() == null || payment.monto().compareTo(order.getTotal()) != 0
                || !Objects.equals(method, payment.metodoPago()) || payment.estado() == null
                || !Set.of("PENDIENTE", "APROBADO", "RECHAZADO").contains(payment.estado()))
            throw new IllegalStateException("Payment devolvió un pago incompatible; revisar antes de recuperar");
        return payment;
    }
}

package com.marketplace.order;

import com.marketplace.order.client.PaymentClient;
import com.marketplace.order.model.Order;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class PaymentClientTest {
    @Test void usaContratoRestRealYRechazaPagoAjeno() throws Exception {
        var body = new AtomicReference<String>();
        var response = new AtomicReference<String>("{\"id\":9,\"orderId\":1,\"userId\":1,\"monto\":700.00,\"metodoPago\":\"TARJETA\",\"estado\":\"PENDIENTE\",\"fechaCreacion\":\"2026-10-27T12:00:00\"}");
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/payments", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            var client = new PaymentClient("http://127.0.0.1:"+server.getAddress().getPort(), 1000, 1000, "TARJETA");
            var order = new Order(); order.setId(1L); order.setUserId(1L); order.setTotal(new BigDecimal("700.00"));
            assertThat(client.crear(order).id()).isEqualTo(9L);
            assertThat(body.get()).contains("\"orderId\":1", "\"userId\":1", "\"monto\":700.00", "\"metodoPago\":\"TARJETA\"");
            response.set(response.get().replace("\"orderId\":1", "\"orderId\":2"));
            assertThatThrownBy(() -> client.crear(order)).isInstanceOf(IllegalStateException.class);
        } finally { server.stop(0); }
    }
}

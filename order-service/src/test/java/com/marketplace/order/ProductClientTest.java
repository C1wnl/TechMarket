package com.marketplace.order;

import com.marketplace.order.client.ProductClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductClientTest {
    @Test void servicioApagadoDevuelve503() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.start();
        server.stop(0);
        var client = new ProductClient(RestClient.builder().baseUrl("http://127.0.0.1:" + port).build(), "demo");
        assertThatThrownBy(() -> client.obtenerProducto(1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getStatusCode().value()).isEqualTo(503));
    }
}

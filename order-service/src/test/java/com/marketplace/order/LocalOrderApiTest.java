package com.marketplace.order;

import com.marketplace.order.security.JwtAuthenticationFilter;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:localorder;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class LocalOrderApiTest {
    static final AtomicInteger calls = new AtomicInteger();
    static final HttpServer products = startProducts();
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;
    @Autowired Environment environment;

    static HttpServer startProducts() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/products/1", exchange -> {
                calls.incrementAndGet();
                byte[] body = "{\"id\":1,\"nombre\":\"Teclado\",\"precio\":350.25,\"stock\":10}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.createContext("/internal/products/", exchange -> {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            });
            server.start();return server;
        } catch (IOException ex) { throw new ExceptionInInitializerError(ex); }
    }

    @DynamicPropertySource
    static void productUrl(DynamicPropertyRegistry registry) {
        registry.add("product.service.url", () -> "http://127.0.0.1:" + products.getAddress().getPort());
    }

    @AfterAll static void stop() { products.stop(0); }

    @Test void creaSinAuthCalculaPorHttpYAsignaUsuarioDeDemostracion() throws Exception {
        assertThat(environment.getDefaultProfiles()).contains("local");
        assertThat(environment.getProperty("eureka.client.enabled")).isEqualTo("false");
        assertThat(context.getBeansOfType(JwtAuthenticationFilter.class)).isEmpty();
        mvc.perform(post("/orders").contentType("application/json")
                .content("{\"userId\":999,\"total\":1,\"items\":[{\"productId\":1,\"cantidad\":2}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.total").value(700.50));
        assertThat(calls.get()).isEqualTo(1);
        mvc.perform(get("/orders")).andExpect(status().isOk());
    }

    @Test void corsPermiteFrontendLocal() throws Exception {
        mvc.perform(options("/orders").header("Origin", "http://localhost:63342")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:63342"));
    }
}

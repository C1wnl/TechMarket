package com.marketplace.order;

import com.marketplace.order.repository.OrderRepository;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.profiles.active=integrado",
        "spring.datasource.url=jdbc:h2:mem:order;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "eureka.client.enabled=false",
        "product.service.read-timeout-ms=600", "product.service.connect-timeout-ms=600",
        "jwt.secret=solo-para-pruebas-123456789012345678901234567890",
        "stock.internal.key=clave-interna-pruebas"})
@AutoConfigureMockMvc
class OrderApiTest {
    static final AtomicInteger calls = new AtomicInteger();
    static final ExecutorService executor = Executors.newCachedThreadPool();
    static final HttpServer products = startProducts();
    @Autowired MockMvc mvc;
    @Autowired OrderRepository repository;

    static HttpServer startProducts() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(executor);
            server.createContext("/products/", exchange -> {
                calls.incrementAndGet();
                String id = exchange.getRequestURI().getPath().substring("/products/".length());
                int status = 200;
                String response;
                switch (id) {
                    case "1" -> response = "{\"id\":1,\"nombre\":\"Teclado\",\"precio\":350.25,\"stock\":10}";
                    case "2" -> response = "{\"id\":2,\"nombre\":\"Mouse\",\"precio\":100.10,\"stock\":4}";
                    case "3" -> response = "{\"id\":3,\"nombre\":\"Agotado\",\"precio\":1,\"stock\":0}";
                    case "4" -> response = "{\"id\":4,\"nombre\":\"Costoso\",\"precio\":9999999999.99,\"stock\":1000}";
                    case "500" -> { status = 500; response = "{}"; }
                    case "88" -> response = "{\"id\":88,\"nombre\":\"Incompleto\"}";
                    case "89" -> response = "invalid json";
                    case "99" -> {
                        try { Thread.sleep(1200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        response = "{}";
                    }
                    default -> { status = 404; response = "{}"; }
                }
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                try {
                    exchange.sendResponseHeaders(status, bytes.length);
                    exchange.getResponseBody().write(bytes);
                } finally { exchange.close(); }
            });
            server.createContext("/internal/products/", exchange -> {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException ex) { throw new ExceptionInInitializerError(ex); }
    }

    @DynamicPropertySource
    static void productUrl(DynamicPropertyRegistry registry) {
        registry.add("product.service.url", () -> "http://127.0.0.1:" + products.getAddress().getPort());
    }

    @BeforeEach void clean() { repository.deleteAll(); calls.set(0); }
    @AfterAll static void stop() { products.stop(0); executor.shutdownNow(); }

    String token(long userId, String role) {
        return "Bearer " + Jwts.builder().subject(String.valueOf(userId)).claim("rol", role)
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor("solo-para-pruebas-123456789012345678901234567890".getBytes(StandardCharsets.UTF_8))).compact();
    }

    String item(long id, int quantity) {
        return "{\"items\":[{\"productId\":" + id + ",\"cantidad\":" + quantity + "}]}";
    }

    @Test void consultaHttpCalculaTotalYUsaIdentidadDelToken() throws Exception {
        mvc.perform(post("/orders").header("Authorization", token(7, "USER"))
                .contentType("application/json")
                .content("{\"userId\":999,\"total\":0.01,\"items\":[{\"productId\":1,\"cantidad\":2},{\"productId\":2,\"cantidad\":3}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.total").value(1000.80)).andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.items[0].nombre").value("Teclado"))
                .andExpect(jsonPath("$.items[0].subtotal").value(700.50));
        assertThat(calls.get()).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(1);
        mvc.perform(get("/orders").header("Authorization", token(7, "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].items.length()").value(2));
    }

    @Test void usuarioSoloVeSusPedidosYAdminVeTodos() throws Exception {
        mvc.perform(post("/orders").header("Authorization", token(7, "USER"))
                .contentType("application/json").content(item(1, 1))).andExpect(status().isCreated());
        long id = repository.findAll().get(0).getId();
        mvc.perform(get("/orders").header("Authorization", token(8, "USER")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/orders/" + id).header("Authorization", token(8, "USER"))).andExpect(status().isNotFound());
        mvc.perform(get("/orders/" + id).header("Authorization", token(7, "USER"))).andExpect(status().isOk());
        mvc.perform(get("/orders").header("Authorization", token(8, "ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/orders/" + id).header("Authorization", token(8, "ADMIN"))).andExpect(status().isOk());
    }

    @Test void rechazaSinTokenYTokenInvalido() throws Exception {
        mvc.perform(post("/orders").contentType("application/json").content(item(1, 1))).andExpect(status().isUnauthorized());
        mvc.perform(get("/orders").header("Authorization", "Bearer falso")).andExpect(status().isUnauthorized());
        assertThat(calls.get()).isZero();
    }

    @Test void rechazaCuerpoAntiguoVacioCantidadesInvalidasYDuplicados() throws Exception {
        for (String body : new String[]{"{}", "{\"items\":[]}", "{\"userId\":1,\"total\":350}",
                "{\"items\":[null]}", item(1, 0), item(1, -1), item(1, 1001), item(0, 1),
                "{\"items\":[{\"productId\":1,\"cantidad\":1},{\"productId\":1,\"cantidad\":1}]}"}) {
            mvc.perform(post("/orders").header("Authorization", token(7, "USER"))
                    .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        assertThat(calls.get()).isZero();
        assertThat(repository.count()).isZero();
    }

    @Test void stockInsuficienteOAgotadoNoGuarda() throws Exception {
        for (String body : new String[]{item(1, 11), item(3, 1)}) {
            mvc.perform(post("/orders").header("Authorization", token(7, "USER"))
                    .contentType("application/json").content(body)).andExpect(status().isConflict());
        }
        assertThat(repository.count()).isZero();
    }

    @Test void productoInexistenteYFalloEnSegundaLineaNoGuardanParcialmente() throws Exception {
        mvc.perform(post("/orders").header("Authorization", token(7, "USER"))
                .contentType("application/json").content("{\"items\":[{\"productId\":1,\"cantidad\":1},{\"productId\":404,\"cantidad\":1}]}"))
                .andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }

    @Test void errorDeProductosDatosIncompletosYTimeout() throws Exception {
        for (long id : new long[]{500, 88, 89}) {
            mvc.perform(post("/orders").header("Authorization", token(7, "USER"))
                    .contentType("application/json").content(item(id, 1))).andExpect(status().isBadGateway());
        }
        mvc.perform(post("/orders").header("Authorization", token(7, "USER"))
                .contentType("application/json").content(item(99, 1))).andExpect(status().isServiceUnavailable());
        assertThat(repository.count()).isZero();
    }

    @Test void limiteMonetarioNoDesbordaLaBase() throws Exception {
        mvc.perform(post("/orders").header("Authorization", token(7, "USER"))
                .contentType("application/json").content(item(4, 2))).andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }
}

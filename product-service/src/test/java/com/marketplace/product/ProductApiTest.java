package com.marketplace.product;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.profiles.active=integrado",
        "spring.datasource.url=jdbc:h2:mem:product;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "eureka.client.enabled=false",
        "jwt.secret=solo-para-pruebas-123456789012345678901234567890",
        "stock.internal.key=clave-interna-pruebas"})
@AutoConfigureMockMvc
class ProductApiTest {
    @Autowired MockMvc mvc;
    private static final String PRODUCT = "{\"nombre\":\"Teclado USB\",\"descripcion\":\"Prueba\",\"precio\":350.25,\"stock\":10}";

    private String token(String role, long expiration) {
        return "Bearer " + Jwts.builder().subject("1").claim("rol", role)
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(Keys.hmacShaKeyFor("solo-para-pruebas-123456789012345678901234567890".getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Test void catalogoPublicoYProductoInexistente() throws Exception {
        mvc.perform(get("/products")).andExpect(status().isOk());
        mvc.perform(get("/products/999999")).andExpect(status().isNotFound());
    }

    @Test void soloAdministradorPuedeCrear() throws Exception {
        mvc.perform(post("/products").contentType("application/json").content(PRODUCT)).andExpect(status().isUnauthorized());
        mvc.perform(post("/products").header("Authorization", token("USER", 60000))
                .contentType("application/json").content(PRODUCT)).andExpect(status().isForbidden());
        mvc.perform(post("/products").header("Authorization", token("ADMIN", 60000))
                .contentType("application/json").content(PRODUCT))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.precio").value(350.25))
                .andExpect(jsonPath("$.stock").value(10));
    }

    @Test void datosInvalidosYTokenExpirado() throws Exception {
        mvc.perform(post("/products").header("Authorization", token("ADMIN", 60000))
                .contentType("application/json").content("{\"nombre\":\"\",\"precio\":-1,\"stock\":-2}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/products").header("Authorization", token("ADMIN", -60000))
                .contentType("application/json").content(PRODUCT)).andExpect(status().isUnauthorized());
        mvc.perform(post("/products").header("Authorization", "Bearer falso")
                .contentType("application/json").content(PRODUCT)).andExpect(status().isUnauthorized());
    }
}

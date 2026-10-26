package com.marketplace.product;

import com.marketplace.product.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:localproduct;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class LocalProductApiTest {
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;
    @Autowired Environment environment;

    @Test void modoPredeterminadoArrancaSinJwtNiEurekaYCreaProductos() throws Exception {
        assertThat(environment.getDefaultProfiles()).contains("local");
        assertThat(environment.getProperty("eureka.client.enabled")).isEqualTo("false");
        assertThat(context.getBeansOfType(JwtAuthenticationFilter.class)).isEmpty();
        mvc.perform(post("/products").contentType("application/json")
                .content("{\"nombre\":\"Teclado local\",\"precio\":350.25,\"stock\":10}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());
        mvc.perform(get("/products")).andExpect(status().isOk());
    }

    @Test void corsPermiteFrontendLocalYRechazaOrigenAjeno() throws Exception {
        mvc.perform(options("/products").header("Origin", "http://127.0.0.1:5500")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5500"));
        mvc.perform(options("/products").header("Origin", "https://ejemplo-ajeno.invalid")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
}

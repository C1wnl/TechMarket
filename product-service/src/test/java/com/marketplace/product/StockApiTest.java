package com.marketplace.product;

import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.profiles.active=local", "spring.datasource.url=jdbc:h2:mem:stocktest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false"})
@AutoConfigureMockMvc
class StockApiTest {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;

    @Test void reservaLiberaYRechazaStockInsuficiente() throws Exception {
        Product p = new Product();
        p.setNombre("Prueba"); p.setPrecio(new BigDecimal("100.00")); p.setStock(10);
        Long id = products.save(p).getId();
        String reserve = "/internal/products/" + id + "/reserve";
        String release = "/internal/products/" + id + "/release";
        mvc.perform(post(reserve).contentType("application/json").content("{\"quantity\":2}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(reserve).header("X-Internal-Key", "techmarket-local-stock-key")
                .contentType("application/json").content("{\"quantity\":2}"))
                .andExpect(status().isNoContent());
        assertThat(products.findById(id).orElseThrow().getStock()).isEqualTo(8);
        mvc.perform(post(reserve).header("X-Internal-Key", "techmarket-local-stock-key")
                .contentType("application/json").content("{\"quantity\":9}"))
                .andExpect(status().isConflict());
        mvc.perform(post(release).header("X-Internal-Key", "techmarket-local-stock-key")
                .contentType("application/json").content("{\"quantity\":2}"))
                .andExpect(status().isNoContent());
        assertThat(products.findById(id).orElseThrow().getStock()).isEqualTo(10);
    }
}

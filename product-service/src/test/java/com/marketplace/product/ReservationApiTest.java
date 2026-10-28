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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties={"spring.profiles.active=local",
    "spring.datasource.url=jdbc:h2:mem:reservationtest;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop", "eureka.client.enabled=false"})
@AutoConfigureMockMvc
class ReservationApiTest {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Test void duplicadosYCompensacionNoAlteranDosVecesElStock() throws Exception {
        Product p = new Product(); p.setNombre("Prueba Saga");
        p.setPrecio(new BigDecimal("100.00")); p.setStock(10);
        Long id = products.save(p).getId();
        String path = "/internal/reservations/123/products/" + id;
        for (int i=0;i<2;i++) mvc.perform(post(path+"/reserve")
                .header("X-Internal-Key","techmarket-local-stock-key")
                .contentType("application/json").content("{\"quantity\":2}"))
                .andExpect(status().isNoContent());
        assertThat(products.findById(id).orElseThrow().getStock()).isEqualTo(8);
        mvc.perform(post(path+"/reserve").header("X-Internal-Key","techmarket-local-stock-key")
                .contentType("application/json").content("{\"quantity\":3}"))
                .andExpect(status().isConflict());
        for (int i=0;i<2;i++) mvc.perform(post(path+"/release")
                .header("X-Internal-Key","techmarket-local-stock-key"))
                .andExpect(status().isNoContent());
        assertThat(products.findById(id).orElseThrow().getStock()).isEqualTo(10);
        mvc.perform(post(path+"/reserve").header("X-Internal-Key","techmarket-local-stock-key")
                .contentType("application/json").content("{\"quantity\":2}"))
                .andExpect(status().isConflict());
    }
    @Test void cancelacionAntesDeReservaEvitaDescuentoTardio() throws Exception {
        Product p = new Product(); p.setNombre("Producto tardío");
        p.setPrecio(new BigDecimal("10")); p.setStock(6);
        Long id = products.save(p).getId();
        String path="/internal/reservations/987/products/"+id;
        mvc.perform(post(path+"/release").header("X-Internal-Key","techmarket-local-stock-key"))
                .andExpect(status().isNoContent());
        mvc.perform(post(path+"/reserve").header("X-Internal-Key","techmarket-local-stock-key")
                .contentType("application/json").content("{\"quantity\":1}"))
                .andExpect(status().isConflict());
        assertThat(products.findById(id).orElseThrow().getStock()).isEqualTo(6);
    }
}

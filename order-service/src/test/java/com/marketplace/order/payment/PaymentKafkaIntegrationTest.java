package com.marketplace.order.payment;

import com.marketplace.order.client.PaymentClient;
import com.marketplace.order.client.ProductClient;
import com.marketplace.order.dto.*;
import com.marketplace.order.model.*;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties = {
    "spring.profiles.active=local,payment-kafka", "payment.recovery.delay-ms=100",
    "spring.datasource.url=jdbc:h2:mem:kafkaintegration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop"})
@EmbeddedKafka(partitions = 1, topics = "payment-events", bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class PaymentKafkaIntegrationTest {
    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired PaymentInboxRepository inbox;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired JsonMapper json;
    @MockitoBean ProductClient products;
    @MockitoBean PaymentClient payments;

    @Test void kafkaEntregaADosGruposYOrderRecuperaResultadosPersistidos() throws Exception {
        when(products.obtenerProducto(1L)).thenReturn(new ProductResponse(1L, "Teclado", new BigDecimal("350.00"), 100));
        when(payments.crear(any())).thenAnswer(a -> {
            Order o = a.getArgument(0);
            return new PaymentClient.PaymentResponse(o.getId()+100, o.getId(), 1L, o.getTotal(), "TARJETA", "PENDIENTE");
        });
        var request = new OrderRequest(List.of(new OrderItemRequest(1L, 2)));
        var approved = service.crearPedido(1L, request);
        var rejected = service.crearPedido(1L, request);
        var a = event(approved, "APROBADO"); var r = event(rejected, "RECHAZADO");
        var producerProps = KafkaTestUtils.producerProps(broker);
        var consumerProps = KafkaTestUtils.consumerProps("notification-independent-test", "false", broker);
        consumerProps.put("auto.offset.reset", "earliest");
        try (var producer = new KafkaProducer<String, String>(producerProps, new StringSerializer(), new StringSerializer());
             var notification = new KafkaConsumer<String, String>(consumerProps, new StringDeserializer(), new StringDeserializer())) {
            notification.subscribe(List.of("payment-events"));
            for (var e : List.of(a, r, a, r)) {
                producer.send(new ProducerRecord<>("payment-events", e.paymentId().toString(), json.writeValueAsString(e))).get(10, TimeUnit.SECONDS);
            }
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(orders.findById(approved.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMADO);
                assertThat(orders.findById(rejected.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELADO);
            });
            int received = 0;
            long end = System.nanoTime() + Duration.ofSeconds(20).toNanos();
            while (received < 4 && System.nanoTime() < end) received += notification.poll(Duration.ofMillis(500)).count();
            assertThat(received).isEqualTo(4);
            assertThat(inbox.count()).isEqualTo(2);
            verify(products, times(1)).confirmar(approved.getId(), 1L);
            verify(products, times(1)).liberar(rejected.getId(), 1L);
        }
    }
    PaymentEvent event(Order o, String status) {
        return new PaymentEvent(UUID.randomUUID().toString(), "APROBADO".equals(status) ? "PAYMENT_APPROVED" : "PAYMENT_REJECTED",
                o.getPaymentId(), o.getId(), 1L, o.getTotal(), status, Instant.now().toString());
    }
}

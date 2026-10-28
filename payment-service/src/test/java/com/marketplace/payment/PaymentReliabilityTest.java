package com.marketplace.payment;

import com.marketplace.payment.model.Payment;
import com.marketplace.payment.repository.PaymentRepository;
import com.marketplace.payment.outbox.*;
import com.marketplace.payment.service.PaymentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:paymentreliability;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop"})
class PaymentReliabilityTest {
    @Autowired PaymentService service;
    @Autowired PaymentRepository payments;
    @Autowired PaymentOutboxRepository outbox;
    @MockitoBean PaymentOutboxPublisher publisher;
    @BeforeEach void clean() { outbox.deleteAll(); payments.deleteAll(); }
    Payment request() {
        var payment = new Payment(); payment.setOrderId(1L); payment.setUserId(1L);
        payment.setMonto(new BigDecimal("700.00")); payment.setMetodoPago("TARJETA"); return payment;
    }
    @Test void reintentoDevuelveMismoPagoInclusoFinalizado() {
        var first = service.crearPago(request()); service.actualizarEstado(first.getId(), "APROBADO");
        var again = service.crearPago(request());
        assertThat(again.getId()).isEqualTo(first.getId());
        assertThat(again.getEstado()).isEqualTo("APROBADO");
        assertThat(payments.count()).isEqualTo(1);
    }
    @Test void mismaOrdenConDatosDistintosEsConflicto() {
        service.crearPago(request());
        for (int field = 0; field < 3; field++) {
            var p = request();
            if (field == 0) p.setUserId(2L);
            if (field == 1) p.setMonto(BigDecimal.ONE);
            if (field == 2) p.setMetodoPago("EFECTIVO");
            assertThatThrownBy(() -> service.crearPago(p)).isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
        }
        assertThat(payments.count()).isEqualTo(1);
    }
    @Test void creacionesConcurrentesNoDuplicanPago() throws Exception {
        try (var pool = Executors.newFixedThreadPool(8)) {
            var start = new CountDownLatch(1); var jobs = new ArrayList<Future<Long>>();
            for (int i = 0; i < 8; i++) jobs.add(pool.submit(() -> { start.await(); return service.crearPago(request()).getId(); }));
            start.countDown(); var ids = new HashSet<Long>();
            for (var job : jobs) ids.add(job.get(20, TimeUnit.SECONDS));
            assertThat(ids).hasSize(1); assertThat(payments.count()).isEqualTo(1);
        }
    }
    @Test void aprobacionRepetidaPublicaUnSoloOutbox() {
        var p = service.crearPago(request()); service.actualizarEstado(p.getId(), "APROBADO");
        service.actualizarEstado(p.getId(), "APROBADO");
        assertThat(outbox.count()).isEqualTo(1);
        assertThat(outbox.findAll().getFirst().getPayload()).contains("PAYMENT_APPROVED");
    }
    @Test void rechazoPublicaContratoExistente() {
        var p = service.crearPago(request()); service.actualizarEstado(p.getId(), "RECHAZADO");
        assertThat(outbox.count()).isEqualTo(1);
        assertThat(outbox.findAll().getFirst().getPayload()).contains("PAYMENT_REJECTED", "eventId", "paymentId", "orderId", "userId", "monto", "estado", "fechaEvento");
    }
    @Test void resultadosConcurrentesNoProducenEventosContradictorios() throws Exception {
        var p = service.crearPago(request());
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1); var jobs = new ArrayList<Future<Boolean>>();
            for (String status : List.of("APROBADO", "RECHAZADO")) {
                jobs.add(pool.submit(() -> {
                    start.await();
                    try { service.actualizarEstado(p.getId(), status); return true; }
                    catch (ResponseStatusException ex) {
                        assertThat(ex.getStatusCode().value()).isEqualTo(409); return false;
                    }
                }));
            }
            start.countDown(); int successes = 0;
            for (var job : jobs) if (job.get(20, TimeUnit.SECONDS)) successes++;
            assertThat(successes).isEqualTo(1); assertThat(outbox.count()).isEqualTo(1);
        }
    }
}

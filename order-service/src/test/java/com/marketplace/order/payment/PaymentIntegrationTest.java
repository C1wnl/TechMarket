package com.marketplace.order.payment;

import com.marketplace.order.client.PaymentClient;
import com.marketplace.order.client.ProductClient;
import com.marketplace.order.dto.OrderRequest;
import com.marketplace.order.dto.OrderItemRequest;
import com.marketplace.order.dto.ProductResponse;
import com.marketplace.order.model.*;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties = {
        "spring.profiles.active=local,payment-kafka", "payment.recovery.enabled=false", "payment.kafka.auto-startup=false",
        "spring.datasource.url=jdbc:h2:mem:paymentintegration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop"})
class PaymentIntegrationTest {
    @Autowired OrderRepository orders;
    @Autowired PaymentInboxRepository inbox;
    @Autowired PaymentInboxService events;
    @Autowired PaymentWorkflow workflow;
    @Autowired OrderService service;
    @Autowired JsonMapper json;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean ProductClient products;
    @MockitoBean PaymentClient payments;

    @BeforeEach void clean() {
        inbox.deleteAll(); orders.deleteAll();
        when(products.obtenerProducto(1L)).thenReturn(new ProductResponse(1L, "Teclado", new BigDecimal("350.00"), 100));
        when(payments.crear(any())).thenAnswer(a -> {
            Order o = a.getArgument(0);
            return new PaymentClient.PaymentResponse(o.getId() + 1000, o.getId(), o.getUserId(), o.getTotal(), "TARJETA", "PENDIENTE");
        });
    }
    OrderRequest request() { return new OrderRequest(List.of(new OrderItemRequest(1L, 2))); }
    Order create() { return service.crearPedido(1L, request()); }
    Order fresh(Order order) { return orders.findById(order.getId()).orElseThrow(); }
    void due(Order order) {
        var o = fresh(order); o.setNextAttempt(Instant.EPOCH); orders.saveAndFlush(o);
    }
    PaymentEvent event(Order o, String status) {
        return new PaymentEvent(UUID.randomUUID().toString(), "APROBADO".equals(status) ? "PAYMENT_APPROVED" : "PAYMENT_REJECTED",
                o.getPaymentId(), o.getId(), o.getUserId(), o.getTotal(), status, Instant.now().toString());
    }
    void receive(PaymentEvent e) { events.receive(json.writeValueAsString(e), UUID.randomUUID().toString()); }
    void apply(PaymentEvent e) { receive(e); events.process(e.eventId()); }

    @Test void creaReservaSolicitaPagoYRelacionaPaymentId() {
        var o = create();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PENDIENTE_PAGO);
        assertThat(o.getPaymentId()).isNotNull();
        verify(products).reservar(o.getId(), 1L, 2);
        verify(payments).crear(any());
    }
    @Test void aprobadoConfirmaInventarioAntesDelPedido() {
        var o = create(); var e = event(o, "APROBADO"); apply(e);
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.PENDIENTE_PAGO);
        workflow.advance(o.getId());
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.CONFIRMADO);
        verify(products).confirmar(o.getId(), 1L);
        verify(products, never()).liberar(anyLong(), anyLong());
        assertThat(inbox.findById(e.eventId()).orElseThrow().getStatus()).isEqualTo("DONE");
    }
    @Test void rechazadoCompensa() {
        var o = create(); apply(event(o, "RECHAZADO")); workflow.advance(o.getId());
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.CANCELADO);
        verify(products).liberar(o.getId(), 1L);
        verify(products, never()).confirmar(anyLong(), anyLong());
    }
    @Test void duplicadoAprobadoNoRepiteConfirmacion() {
        var o = create(); var e = event(o, "APROBADO");
        apply(e); workflow.advance(o.getId()); apply(e); workflow.advance(o.getId());
        apply(event(o, "APROBADO")); workflow.advance(o.getId());
        verify(products, times(1)).confirmar(o.getId(), 1L);
    }
    @Test void duplicadoRechazadoNoRepiteLiberacion() {
        var o = create(); var e = event(o, "RECHAZADO");
        apply(e); workflow.advance(o.getId()); apply(e); workflow.advance(o.getId());
        apply(event(o, "RECHAZADO")); workflow.advance(o.getId());
        verify(products, times(1)).liberar(o.getId(), 1L);
    }
    @Test void rechazoAtrasadoNoCancelaConfirmado() {
        var o = create(); apply(event(o, "APROBADO")); workflow.advance(o.getId());
        apply(event(o, "RECHAZADO")); workflow.advance(o.getId());
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.CONFIRMADO);
        verify(products, never()).liberar(anyLong(), anyLong());
    }
    @Test void aprobadoAtrasadoNoReviveCancelado() {
        var o = create(); apply(event(o, "RECHAZADO")); workflow.advance(o.getId());
        apply(event(o, "APROBADO")); workflow.advance(o.getId());
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.CANCELADO);
        verify(products, never()).confirmar(anyLong(), anyLong());
    }
    @Test void falloConfirmandoPersisteDecisionYReintentaSinAceptarRechazo() {
        var o = create();
        doThrow(new IllegalStateException("Product caído")).doNothing().when(products).confirmar(o.getId(), 1L);
        apply(event(o, "APROBADO")); workflow.advance(o.getId());
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.PENDIENTE_PAGO);
        assertThat(fresh(o).getLastIntegrationError()).contains("Product caído");
        apply(event(o, "RECHAZADO")); due(o);
        // Una nueva instancia recupera exclusivamente el estado persistido, sin memoria del proceso anterior.
        new PaymentWorkflow(orders, products, payments, manager).advance(o.getId());
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.CONFIRMADO);
        verify(products, never()).liberar(anyLong(), anyLong());
    }
    @Test void falloLiberandoQuedaCompensacionPendienteYSeRecupera() {
        var o = create();
        doThrow(new IllegalStateException("temporal")).doNothing().when(products).liberar(o.getId(), 1L);
        apply(event(o, "RECHAZADO")); workflow.advance(o.getId());
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.COMPENSACION_PENDIENTE);
        due(o); workflow.advance(o.getId());
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.CANCELADO);
    }
    @Test void pagoInciertoReintentaMismoPedidoSinLiberarInventario() {
        doThrow(new IllegalStateException("respuesta perdida")).when(payments).crear(any());
        var o = create();
        assertThat(o.getIntegrationPhase()).isEqualTo("CREATE_PAYMENT");
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PENDIENTE);
        doReturn(new PaymentClient.PaymentResponse(999L, o.getId(), 1L, o.getTotal(), "TARJETA", "PENDIENTE")).when(payments).crear(any());
        due(o); workflow.advance(o.getId());
        assertThat(fresh(o).getPaymentId()).isEqualTo(999L);
        verify(products, never()).liberar(anyLong(), anyLong());
    }
    @Test void eventoLlegaAntesDeAsociacionRestYSeConserva() {
        doThrow(new IllegalStateException("timeout")).when(payments).crear(any());
        var o = create();
        var e = new PaymentEvent(UUID.randomUUID().toString(), "PAYMENT_APPROVED", 999L, o.getId(), 1L, o.getTotal(), "APROBADO", Instant.now().toString());
        apply(e);
        assertThat(inbox.findById(e.eventId()).orElseThrow().getStatus()).isEqualTo("PENDING");
        doReturn(new PaymentClient.PaymentResponse(999L, o.getId(), 1L, o.getTotal(), "TARJETA", "APROBADO")).when(payments).crear(any());
        due(o); workflow.advance(o.getId());
        var row = inbox.findById(e.eventId()).orElseThrow(); row.nextAttempt = Instant.EPOCH; inbox.saveAndFlush(row);
        events.process(e.eventId()); workflow.advance(o.getId());
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.CONFIRMADO);
    }
    @Test void validaMontoUsuarioPaymentIdYPertenencia() {
        var o = create(); var other = create();
        var eventsToReject = List.of(
            new PaymentEvent(UUID.randomUUID().toString(), "PAYMENT_APPROVED", o.getPaymentId(), o.getId(), 1L, BigDecimal.ONE, "APROBADO", Instant.now().toString()),
            new PaymentEvent(UUID.randomUUID().toString(), "PAYMENT_APPROVED", o.getPaymentId(), o.getId(), 2L, o.getTotal(), "APROBADO", Instant.now().toString()),
            new PaymentEvent(UUID.randomUUID().toString(), "PAYMENT_APPROVED", o.getPaymentId()+123, o.getId(), 1L, o.getTotal(), "APROBADO", Instant.now().toString()),
            new PaymentEvent(UUID.randomUUID().toString(), "PAYMENT_APPROVED", o.getPaymentId(), other.getId(), 1L, o.getTotal(), "APROBADO", Instant.now().toString()));
        for (var e : eventsToReject) {
            apply(e); assertThat(inbox.findById(e.eventId()).orElseThrow().getStatus()).isEqualTo("INVALID");
        }
        assertThat(fresh(o).getPaymentOutcome()).isNull();
        verify(products, never()).confirmar(anyLong(), anyLong());
    }
    @Test void mensajeMalformadoSeConservaSinBloquearValidos() {
        events.receive("no es JSON", "payment-events:0:1");
        var o = create(); apply(event(o, "APROBADO")); workflow.advance(o.getId());
        assertThat(inbox.findById("invalid:payment-events:0:1").orElseThrow().getStatus()).isEqualTo("INVALID");
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.CONFIRMADO);
    }
    @Test void reservaInciertaCompensaTodasLasLineas() {
        doThrow(new IllegalStateException("respuesta perdida")).when(products).reservar(anyLong(), anyLong(), anyInt());
        var o = create();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.COMPENSACION_PENDIENTE);
        due(o); workflow.advance(o.getId());
        verify(products).liberar(o.getId(), 1L);
        verify(payments, never()).crear(any());
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.CANCELADO);
    }
    @Test void reintentoHttpConMismaClaveNoCreaOtroPedidoNiPago() {
        var one = service.crearPedido(1L, request(), "postman-001");
        var two = service.crearPedido(1L, request(), "postman-001");
        assertThat(two.getId()).isEqualTo(one.getId());
        assertThat(orders.count()).isEqualTo(1);
        verify(payments, times(1)).crear(any());
        assertThatThrownBy(() -> service.crearPedido(1L, new OrderRequest(List.of(new OrderItemRequest(1L, 3))), "postman-001"))
                .isInstanceOf(ResponseStatusException.class);
    }
    @Test void simuladoresNoAlteranPedidosConPagoReal() {
        var o = create();
        assertThatThrownBy(() -> service.aprobarPago(o.getId())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.rechazarPago(o.getId())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.solicitarPago(o.getId())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void confirmacionParcialNuncaMarcaConfirmadoNiPermiteCompensacionContraria() {
        when(products.obtenerProducto(2L)).thenReturn(new ProductResponse(2L, "Mouse", new BigDecimal("100.00"), 20));
        var o = service.crearPedido(1L, new OrderRequest(List.of(new OrderItemRequest(1L, 1), new OrderItemRequest(2L, 1))));
        doThrow(new IllegalStateException("segunda línea caída")).doNothing().when(products).confirmar(o.getId(), 2L);
        apply(event(o, "APROBADO")); workflow.advance(o.getId());
        verify(products).confirmar(o.getId(), 1L);
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.PENDIENTE_PAGO);
        apply(event(o, "RECHAZADO")); due(o); workflow.advance(o.getId());
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.CONFIRMADO);
        verify(products, never()).liberar(anyLong(), anyLong());
    }
    @Test void consumidoresConcurrentesSoloAplicanUnResultado() throws Exception {
        var o = create(); var e = event(o, "APROBADO"); receive(e);
        try (var pool = Executors.newFixedThreadPool(4)) {
            var jobs = new ArrayList<Future<?>>();
            for (int i = 0; i < 8; i++) jobs.add(pool.submit(() -> { events.process(e.eventId()); workflow.advance(o.getId()); }));
            for (var job : jobs) job.get(20, TimeUnit.SECONDS);
        }
        assertThat(fresh(o).getStatus()).isEqualTo(OrderStatus.CONFIRMADO);
        verify(products, times(1)).confirmar(o.getId(), 1L);
    }
}

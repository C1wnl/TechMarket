package com.marketplace.order.payment;

import com.marketplace.order.client.PaymentClient;
import com.marketplace.order.client.ProductClient;
import com.marketplace.order.dto.OrderRequest;
import com.marketplace.order.model.Order;
import com.marketplace.order.model.OrderStatus;
import com.marketplace.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Objects;

@Service
@ConditionalOnProperty(name = "payment.integration.enabled", havingValue = "true")
public class PaymentWorkflow {
    private static final Logger log = LoggerFactory.getLogger(PaymentWorkflow.class);
    private final OrderRepository orders;
    private final ProductClient products;
    private final PaymentClient payments;
    private final TransactionTemplate tx;
    public PaymentWorkflow(OrderRepository orders, ProductClient products, PaymentClient payments,
                           PlatformTransactionManager manager) {
        this.orders = orders; this.products = products; this.payments = payments;
        this.tx = new TransactionTemplate(manager);
    }
    public String key(Long userId, String key) {
        if (key == null) return null;
        if (!key.matches("[A-Za-z0-9._:-]{1,100}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key inválida (1-100 caracteres ASCII)");
        return userId + ":" + key;
    }
    public Order existing(String key, OrderRequest request) {
        var old = orders.findByRequestKey(key).orElse(null);
        if (old == null) return null;
        boolean same = old.getItems().size() == request.items().size()
                && request.items().stream().allMatch(r -> old.getItems().stream().anyMatch(i ->
                Objects.equals(i.getProductId(), r.productId()) && Objects.equals(i.getCantidad(), r.cantidad())))
                && request.items().stream().map(r -> r.productId()).distinct().count() == request.items().size();
        if (!same) throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key utilizada con otros productos/cantidades");
        return old;
    }
    public Order start(Order order, OrderRequest request) {
        order.setIntegrationPhase("RESERVE");
        order.setNextAttempt(Instant.now());
        Long id;
        try {
            // El pedido y la intención se confirman ANTES de cualquier efecto HTTP.
            id = tx.execute(s -> orders.saveAndFlush(order).getId());
        } catch (DataIntegrityViolationException ex) {
            if (order.getRequestKey() != null) {
                var existing = existing(order.getRequestKey(), request);
                if (existing != null) return existing;
            }
            throw ex;
        }
        advance(id); // reserva -> intención de pago (o compensación)
        advance(id); // crea pago -> espera evento (o libera reservas)
        return orders.findById(id).orElseThrow();
    }
    public void advance(Long id) {
        tx.executeWithoutResult(s -> {
            var order = orders.lockById(id).orElseThrow();
            String phase = order.getIntegrationPhase();
            if (phase == null || "DONE".equals(phase) || "WAIT_RESULT".equals(phase)
                    || order.getNextAttempt().isAfter(Instant.now())) return;
            try {
                switch (phase) {
                    case "RESERVE" -> {
                        for (var item : order.getItems())
                            products.reservar(id, item.getProductId(), item.getCantidad());
                        order.setIntegrationPhase("CREATE_PAYMENT");
                    }
                    case "CREATE_PAYMENT" -> {
                        var payment = payments.crear(order);
                        order.setPaymentId(payment.id());
                        order.setStatus(OrderStatus.PENDIENTE_PAGO);
                        order.setIntegrationPhase("WAIT_RESULT");
                    }
                    case "CONFIRM" -> {
                        for (var item : order.getItems()) products.confirmar(id, item.getProductId());
                        order.setStatus(OrderStatus.CONFIRMADO);
                        order.setIntegrationPhase("DONE");
                    }
                    case "RELEASE" -> {
                        RuntimeException error = null;
                        // Incluir todas las líneas: una respuesta de reserva pudo perderse.
                        for (var item : order.getItems()) {
                            try { products.liberar(id, item.getProductId()); }
                            catch (RuntimeException ex) { if (error == null) error = ex; else error.addSuppressed(ex); }
                        }
                        if (error != null) throw error;
                        order.setStatus(OrderStatus.CANCELADO);
                        order.setIntegrationPhase("DONE");
                    }
                    default -> throw new IllegalStateException("Fase desconocida: " + phase);
                }
                order.setLastIntegrationError(null);
                order.setNextAttempt(Instant.now());
            } catch (RuntimeException ex) {
                if ("RESERVE".equals(phase)) {
                    order.setIntegrationPhase("RELEASE");
                    order.setStatus(OrderStatus.COMPENSACION_PENDIENTE);
                }
                int attempts = order.getIntegrationAttempts() + 1;
                order.setIntegrationAttempts(attempts);
                order.setLastIntegrationError(error(ex));
                order.setNextAttempt(Instant.now().plusSeconds(delay(attempts)));
                log.warn("Pedido {} fase {} pendiente; intento {}: {}", id, phase, attempts, error(ex));
            }
            // Los fallos de DB NO se absorben: rollback y recuperación desde la fase persistida.
            orders.saveAndFlush(order);
        });
    }
    static long delay(int attempts) { return Math.min(60, 1L << Math.min(attempts, 6)); }
    static String error(Exception ex) {
        String text = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return text.substring(0, Math.min(text.length(), 1000));
    }
}

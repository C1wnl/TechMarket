package com.marketplace.order.payment;

import com.marketplace.order.client.PaymentClient;
import com.marketplace.order.client.ProductClient;
import com.marketplace.order.dto.OrderRequest;
import com.marketplace.order.model.Order;
import com.marketplace.order.model.OrderStatus;
import com.marketplace.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
@ConditionalOnProperty(
        name = "payment.integration.enabled",
        havingValue = "true"
)
public class PaymentWorkflow {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentWorkflow.class);

    private final OrderRepository orders;
    private final ProductClient products;
    private final PaymentClient payments;
    private final TransactionTemplate tx;
    private final long leaseMillis;

    private record Work(
            String phase,
            Order order,
            Instant leaseUntil
    ) {}

    public PaymentWorkflow(
            OrderRepository orders,
            ProductClient products,
            PaymentClient payments,
            PlatformTransactionManager manager,
            @Value("${payment.workflow.lease-ms:300000}") long leaseMillis
    ) {
        if (leaseMillis <= 0) {
            throw new IllegalArgumentException(
                    "El tiempo de reserva debe ser positivo"
            );
        }

        this.orders = orders;
        this.products = products;
        this.payments = payments;
        this.tx = new TransactionTemplate(manager);
        this.leaseMillis = leaseMillis;
    }

    public String key(Long userId, String key) {
        if (key == null) {
            return null;
        }

        if (!key.matches("[A-Za-z0-9._:-]{1,100}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key inválida (1-100 caracteres ASCII)"
            );
        }

        return userId + ":" + key;
    }

    public Order existing(String key, OrderRequest request) {
        var old = orders.findByRequestKey(key).orElse(null);

        if (old == null) {
            return null;
        }

        boolean same = old.getItems().size() == request.items().size()
                && request.items().stream().allMatch(r ->
                old.getItems().stream().anyMatch(i ->
                        Objects.equals(i.getProductId(), r.productId())
                                && Objects.equals(i.getCantidad(), r.cantidad())
                ))
                && request.items().stream()
                .map(r -> r.productId())
                .distinct()
                .count() == request.items().size();

        if (!same) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency-Key utilizada con otros productos/cantidades"
            );
        }

        return old;
    }

    public Order start(Order order, OrderRequest request) {
        order.setIntegrationPhase("RESERVE");
        order.setNextAttempt(Instant.now());

        Long id;

        try {
            id = tx.execute(status ->
                    orders.saveAndFlush(order).getId()
            );

        } catch (DataIntegrityViolationException ex) {
            if (order.getRequestKey() != null) {
                var old = existing(order.getRequestKey(), request);

                if (old != null) {
                    return old;
                }
            }

            throw ex;
        }

        // Primera fase: reservar inventario.
        advance(id);

        // Segunda fase: crear el pago si la reserva terminó correctamente.
        advance(id);

        return orders.findById(id).orElseThrow();
    }

    public void advance(Long id) {

        // 1. Reclamar el trabajo mediante una transacción corta.
        Work work = tx.execute(status -> {
            var order = orders.lockById(id).orElseThrow();
            Instant now = Instant.now();

            String phase = order.getIntegrationPhase();

            if (phase == null
                    || "DONE".equals(phase)
                    || "WAIT_RESULT".equals(phase)
                    || (order.getNextAttempt() != null
                    && order.getNextAttempt().isAfter(now))) {
                return null;
            }

            Instant leaseUntil = now.plusMillis(leaseMillis);

            order.setNextAttempt(leaseUntil);
            orders.saveAndFlush(order);

            return new Work(
                    phase,
                    order,
                    leaseUntil
            );
        });

        if (work == null) {
            return;
        }

        // 2. Ejecutar las llamadas HTTP fuera de la transacción.
        PaymentClient.PaymentResponse payment;

        try {
            payment = execute(work);

        } catch (RuntimeException ex) {
            fail(work, ex);
            return;
        }

        // 3. Guardar el resultado en una nueva transacción.
        complete(work, payment);
    }

    private PaymentClient.PaymentResponse execute(Work work) {
        Long orderId = work.order().getId();

        switch (work.phase()) {

            case "RESERVE" -> {
                for (var item : work.order().getItems()) {
                    products.reservar(
                            orderId,
                            item.getProductId(),
                            item.getCantidad()
                    );
                }

                return null;
            }

            case "CREATE_PAYMENT" -> {
                return payments.crear(work.order());
            }

            case "CONFIRM" -> {
                for (var item : work.order().getItems()) {
                    products.confirmar(
                            orderId,
                            item.getProductId()
                    );
                }

                return null;
            }

            case "RELEASE" -> {
                RuntimeException error = null;

                // Intentamos liberar todas las reservas.
                for (var item : work.order().getItems()) {
                    try {
                        products.liberar(
                                orderId,
                                item.getProductId()
                        );

                    } catch (RuntimeException ex) {
                        if (error == null) {
                            error = ex;
                        } else {
                            error.addSuppressed(ex);
                        }
                    }
                }

                if (error != null) {
                    throw error;
                }

                return null;
            }

            default -> throw new IllegalStateException(
                    "Fase desconocida: " + work.phase()
            );
        }
    }

    private void complete(
            Work work,
            PaymentClient.PaymentResponse payment
    ) {
        tx.executeWithoutResult(status -> {
            var order = orders.lockById(
                    work.order().getId()
            ).orElseThrow();

            // Evita que un proceso antiguo sobrescriba otro resultado.
            if (!owns(order, work)) {
                return;
            }

            switch (work.phase()) {

                case "RESERVE" ->
                        order.setIntegrationPhase("CREATE_PAYMENT");

                case "CREATE_PAYMENT" -> {
                    if (payment == null
                            || payment.id() == null
                            || payment.id() <= 0) {
                        throw new IllegalStateException(
                                "Payment no devolvió un identificador válido"
                        );
                    }

                    order.setPaymentId(payment.id());
                    order.setStatus(OrderStatus.PENDIENTE_PAGO);
                    order.setIntegrationPhase("WAIT_RESULT");
                }

                case "CONFIRM" -> {
                    order.setStatus(OrderStatus.CONFIRMADO);
                    order.setIntegrationPhase("DONE");
                }

                case "RELEASE" -> {
                    order.setStatus(OrderStatus.CANCELADO);
                    order.setIntegrationPhase("DONE");
                }

                default -> throw new IllegalStateException(
                        "Fase desconocida al completar: " + work.phase()
                );
            }

            order.setLastIntegrationError(null);
            order.setNextAttempt(Instant.now());

            orders.saveAndFlush(order);
        });
    }

    private void fail(Work work, RuntimeException ex) {
        tx.executeWithoutResult(status -> {
            var order = orders.lockById(
                    work.order().getId()
            ).orElseThrow();

            if (!owns(order, work)) {
                return;
            }

            // Una reserva pudo haberse realizado aunque otra fallara.
            if ("RESERVE".equals(work.phase())) {
                order.setIntegrationPhase("RELEASE");
                order.setStatus(
                        OrderStatus.COMPENSACION_PENDIENTE
                );
            }

            int attempts = Objects.requireNonNullElse(
                    order.getIntegrationAttempts(),
                    0
            ) + 1;

            order.setIntegrationAttempts(attempts);
            order.setLastIntegrationError(error(ex));
            order.setNextAttempt(
                    Instant.now().plusSeconds(delay(attempts))
            );

            log.warn(
                    "Pedido {} fase {} pendiente; intento {}: {}",
                    order.getId(),
                    work.phase(),
                    attempts,
                    error(ex)
            );

            orders.saveAndFlush(order);
        });
    }

    private boolean owns(Order order, Work work) {
        return Objects.equals(
                order.getIntegrationPhase(),
                work.phase()
        )
                && order.getNextAttempt() != null
                && order.getNextAttempt().toEpochMilli()
                == work.leaseUntil().toEpochMilli();
    }

    static long delay(int attempts) {
        return Math.min(
                60,
                1L << Math.min(attempts, 6)
        );
    }

    static String error(Exception ex) {
        String message = ex.getClass().getSimpleName()
                + ": " + ex.getMessage();

        return message.substring(
                0,
                Math.min(message.length(), 1000)
        );
    }
}

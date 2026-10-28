package com.marketplace.order.payment;

import com.marketplace.order.model.OrderStatus;
import com.marketplace.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "payment.integration.enabled", havingValue = "true")
public class PaymentInboxService {
    private static final Logger log = LoggerFactory.getLogger(PaymentInboxService.class);
    private final PaymentInboxRepository inbox;
    private final OrderRepository orders;
    private final JsonMapper json;
    private final TransactionTemplate tx;
    private final EntityManager em;
    public PaymentInboxService(PaymentInboxRepository inbox, OrderRepository orders, JsonMapper json,
                               PlatformTransactionManager manager, EntityManager em) {
        this.inbox = inbox; this.orders = orders; this.json = json;
        this.tx = new TransactionTemplate(manager); this.em = em;
    }
    public void receive(String payload, String deliveryKey) {
        var entry = new PaymentInbox("invalid:" + deliveryKey, payload == null ? "null" : payload);
        try {
            var event = json.readValue(entry.payload, PaymentEvent.class);
            validate(event);
            entry.id = event.eventId();
        } catch (RuntimeException ex) {
            entry.status = "INVALID";
            entry.lastError = PaymentWorkflow.error(ex);
        }
        if (inbox.existsById(entry.id)) return;
        try {
            // persist (no merge): una entrega concurrente nunca sobrescribe DONE con PENDING.
            tx.executeWithoutResult(s -> { em.persist(entry); em.flush(); });
        } catch (RuntimeException ex) {
            if (!inbox.existsById(entry.id)) throw ex;
        }
        if ("INVALID".equals(entry.status)) log.error("Evento inválido conservado en inbox {}: {}", entry.id, entry.lastError);
    }
    public void process(String id) {
        tx.executeWithoutResult(s -> {
            var entry = inbox.lockById(id).orElseThrow();
            if (!"PENDING".equals(entry.status) || entry.nextAttempt.isAfter(Instant.now())) return;
            var event = json.readValue(entry.payload, PaymentEvent.class);
            var order = orders.lockById(event.orderId()).orElse(null);
            if (order == null || order.getIntegrationPhase() == null) {
                finish(entry, "IGNORED", "Pedido inexistente o histórico sin integración"); return;
            }
            if (order.getPaymentId() == null) {
                if ("RESERVE".equals(order.getIntegrationPhase()) || "CREATE_PAYMENT".equals(order.getIntegrationPhase())) {
                    entry.attempts++;
                    entry.nextAttempt = Instant.now().plusSeconds(PaymentWorkflow.delay(entry.attempts));
                    entry.lastError = "Esperando asociación REST con paymentId";
                    return;
                }
                finish(entry, "INVALID", "El pedido no tiene pago asociado"); return;
            }
            if (!Objects.equals(event.paymentId(), order.getPaymentId())
                    || !Objects.equals(event.userId(), order.getUserId())
                    || event.monto().compareTo(order.getTotal()) != 0) {
                finish(entry, "INVALID", "paymentId, usuario o monto no corresponde al pedido"); return;
            }
            // Se fija una sola decisión ANTES de cualquier confirmación/liberación HTTP.
            if (order.getPaymentOutcome() != null) {
                finish(entry, "IGNORED", "Resultado ya fijado: " + order.getPaymentOutcome()); return;
            }
            if (order.getStatus() != OrderStatus.PENDIENTE_PAGO || !"WAIT_RESULT".equals(order.getIntegrationPhase())) {
                finish(entry, "IGNORED", "Pedido en estado terminal o incompatible"); return;
            }
            order.setPaymentOutcome(event.estado());
            order.setIntegrationPhase("APROBADO".equals(event.estado()) ? "CONFIRM" : "RELEASE");
            if ("RECHAZADO".equals(event.estado())) order.setStatus(OrderStatus.COMPENSACION_PENDIENTE);
            order.setNextAttempt(Instant.now());
            finish(entry, "DONE", null);
        });
    }
    private void finish(PaymentInbox entry, String status, String error) {
        entry.status = status; entry.lastError = error;
        if (error != null) log.warn("Evento {} {}: {}", entry.id, status, error);
    }
    private void validate(PaymentEvent event) {
        if (event == null || event.eventId() == null) throw new IllegalArgumentException("Falta eventId");
        if (!UUID.fromString(event.eventId()).toString().equalsIgnoreCase(event.eventId()))
            throw new IllegalArgumentException("eventId no es UUID canónico");
        if (event.orderId() == null || event.orderId() <= 0 || event.paymentId() == null || event.paymentId() <= 0
                || event.userId() == null || event.userId() <= 0 || event.monto() == null || event.monto().signum() <= 0)
            throw new IllegalArgumentException("Identidad o monto inválido");
        if (!("PAYMENT_APPROVED".equals(event.eventType()) && "APROBADO".equals(event.estado()))
                && !("PAYMENT_REJECTED".equals(event.eventType()) && "RECHAZADO".equals(event.estado())))
            throw new IllegalArgumentException("Tipo y estado incompatibles");
        Instant.parse(event.fechaEvento());
    }
}

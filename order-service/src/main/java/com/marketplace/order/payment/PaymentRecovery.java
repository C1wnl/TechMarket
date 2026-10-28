package com.marketplace.order.payment;

import com.marketplace.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Instant;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = {"payment.integration.enabled", "payment.recovery.enabled"}, havingValue = "true")
public class PaymentRecovery {
    private static final Logger log = LoggerFactory.getLogger(PaymentRecovery.class);
    private final PaymentInboxRepository inbox;
    private final PaymentInboxService events;
    private final OrderRepository orders;
    private final PaymentWorkflow workflow;
    public PaymentRecovery(PaymentInboxRepository inbox, PaymentInboxService events,
                           OrderRepository orders, PaymentWorkflow workflow) {
        this.inbox = inbox; this.events = events; this.orders = orders; this.workflow = workflow;
    }
    @Scheduled(fixedDelayString = "${payment.recovery.delay-ms:2000}", initialDelayString = "${payment.recovery.delay-ms:2000}")
    public void recover() {
        for (var id : inbox.findPending(Instant.now(), PageRequest.of(0, 50))) {
            try { events.process(id); }
            catch (RuntimeException ex) { log.error("Inbox {} sigue pendiente: {}", id, PaymentWorkflow.error(ex)); }
        }
        for (var id : orders.findRecoverable(Instant.now(), PageRequest.of(0, 50))) {
            try { workflow.advance(id); }
            catch (RuntimeException ex) { log.error("Pedido {} sigue pendiente: {}", id, PaymentWorkflow.error(ex)); }
        }
    }
}

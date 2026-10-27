
package com.marketplace.payment.outbox;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_outbox")
public class PaymentOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean enviado = false;

    @Column(nullable = false)
    private Instant fechaCreacion = Instant.now();

    protected PaymentOutboxEvent() {
    }

    public PaymentOutboxEvent(
            String eventId,
            Long paymentId,
            String payload) {

        this.eventId = eventId;
        this.paymentId = paymentId;
        this.payload = payload;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isEnviado() {
        return enviado;
    }

    public void setEnviado(boolean enviado) {
        this.enviado = enviado;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
}

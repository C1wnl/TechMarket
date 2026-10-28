package com.marketplace.order.payment;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "order_payment_inbox", indexes = @Index(name = "idx_order_inbox_pending", columnList = "status,next_attempt"))
public class PaymentInbox {
    @Id @Column(length = 200)
    String id;
    @Column(nullable = false, columnDefinition = "TEXT")
    String payload;
    @Column(nullable = false)
    String status = "PENDING";
    Instant receivedAt = Instant.now();
    @Column(name = "next_attempt")
    Instant nextAttempt = Instant.now();
    int attempts;
    @Column(length = 1000)
    String lastError;
    protected PaymentInbox() {}
    PaymentInbox(String id, String payload) { this.id = id; this.payload = payload; }
    public String getId() { return id; }
    public String getStatus() { return status; }
    public String getLastError() { return lastError; }
}

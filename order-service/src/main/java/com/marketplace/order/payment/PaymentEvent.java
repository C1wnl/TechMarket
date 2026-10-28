package com.marketplace.order.payment;

import java.math.BigDecimal;

// Contrato idéntico a Luis-payment; fechaEvento es String en el productor.
public record PaymentEvent(String eventId, String eventType, Long paymentId, Long orderId,
                           Long userId, BigDecimal monto, String estado, String fechaEvento) {}

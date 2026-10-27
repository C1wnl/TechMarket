
package com.marketplace.payment.event;

import java.math.BigDecimal;

public record PaymentEvent(
        String eventId,
        String eventType,
        Long paymentId,
        Long orderId,
        Long userId,
        BigDecimal monto,
        String estado,
        String fechaEvento
) {}

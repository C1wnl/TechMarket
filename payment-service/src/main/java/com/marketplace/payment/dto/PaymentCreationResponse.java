
package com.marketplace.payment.dto;

import java.math.BigDecimal;

public record PaymentCreationResponse(
        Long id,
        Long orderId,
        Long userId,
        BigDecimal monto,
        String metodoPago,
        String estado
) {}

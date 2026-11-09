package com.marketplace.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long numeroPedido,
        BigDecimal monto,
        String metodoPago,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {}
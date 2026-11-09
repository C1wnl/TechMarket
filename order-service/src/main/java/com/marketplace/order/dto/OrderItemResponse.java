package com.marketplace.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String nombre,
        BigDecimal precioUnitario,
        Integer cantidad,
        BigDecimal subtotal
) {
}

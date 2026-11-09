package com.marketplace.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminOrderResponse(
        Long numeroPedido,
        String nombreCliente,
        String emailCliente,
        String estado,
        BigDecimal total,
        Instant fechaCreacion,
        List<OrderItemResponse> items
) {}

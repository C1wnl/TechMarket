package com.marketplace.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotNull @Positive Long productId,
        @NotNull @Positive @Max(value = 1000, message = "Máximo 1000 unidades por producto")
        Integer cantidad) { }

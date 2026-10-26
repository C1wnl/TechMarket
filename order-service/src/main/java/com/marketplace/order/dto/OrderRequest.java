package com.marketplace.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "Agrega al menos un producto")
        @Size(max = 50, message = "El pedido admite hasta 50 productos")
        List<@NotNull @Valid OrderItemRequest> items) { }

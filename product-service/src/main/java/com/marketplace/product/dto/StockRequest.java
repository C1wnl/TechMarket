package com.marketplace.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockRequest(@NotNull @Positive Integer quantity) { }

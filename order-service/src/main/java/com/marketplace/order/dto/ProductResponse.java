package com.marketplace.order.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductResponse(Long id, String nombre, BigDecimal precio, Integer stock) { }

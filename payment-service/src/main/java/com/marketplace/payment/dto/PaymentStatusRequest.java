package com.marketplace.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class PaymentStatusRequest {

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}


package com.marketplace.payment.dto;

public record NotificationRequest(
        Long userId,
        String tipo,
        String mensaje
) {}

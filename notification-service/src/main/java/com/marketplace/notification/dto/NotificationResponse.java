package com.marketplace.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String tipo,
        String mensaje,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {}

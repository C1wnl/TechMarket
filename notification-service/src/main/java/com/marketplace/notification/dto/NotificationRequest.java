
package com.marketplace.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NotificationRequest(

        @NotNull
        @Positive
        Long userId,

        @NotBlank
        String tipo,

        @NotBlank
        String mensaje

) {}

package com.marketplace.notification.controller;

import com.marketplace.notification.dto.NotificationRequest;
import com.marketplace.notification.dto.NotificationResponse;
import com.marketplace.notification.model.Notification;
import com.marketplace.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse crear(
            @Valid @RequestBody NotificationRequest request
    ) {
        Notification notification = new Notification();

        notification.setUserId(request.userId());
        notification.setTipo(request.tipo());
        notification.setMensaje(request.mensaje());

        return convertirRespuesta(notificationService.crear(notification));
    }

    @GetMapping
    public List<NotificationResponse> obtenerTodas() {
        return notificationService.obtenerTodas()
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @GetMapping("/{id}")
    public NotificationResponse obtenerPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = obtenerUserId(authentication);

        Notification notification = notificationService.obtenerPorId(
                id, userId, esAdmin(authentication)
        );

        return convertirRespuesta(notification);
    }

    @GetMapping("/user/{userId}")
    public List<NotificationResponse> obtenerPorUsuario(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long userIdAutenticado = obtenerUserId(authentication);

        return notificationService.obtenerPorUsuario(
                        userId, userIdAutenticado, esAdmin(authentication)
                )
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @PutMapping("/{id}/status")
    public NotificationResponse actualizarEstado(
            @PathVariable Long id,
            @RequestBody StatusRequest request,
            Authentication authentication
    ) {
        if (request == null || request.estado() == null
                || request.estado().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El estado es obligatorio"
            );
        }

        String estado = request.estado().trim().toUpperCase();

        if (!estado.equals("PENDIENTE")
                && !estado.equals("ENVIADA")
                && !estado.equals("LEIDA")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado no válido"
            );
        }

        Notification notification = notificationService.actualizarEstado(
                id,
                estado,
                obtenerUserId(authentication),
                esAdmin(authentication)
        );

        return convertirRespuesta(notification);
    }

    private Long obtenerUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private NotificationResponse convertirRespuesta(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTipo(),
                notification.getMensaje(),
                notification.getEstado(),
                notification.getFechaCreacion(),
                notification.getFechaActualizacion()
        );
    }

    public record StatusRequest(String estado) {}
}

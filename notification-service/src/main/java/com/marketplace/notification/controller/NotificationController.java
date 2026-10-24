
package com.marketplace.notification.controller;

import com.marketplace.notification.model.Notification;
import com.marketplace.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;
import com.marketplace.notification.dto.NotificationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public Notification crear(@Valid @RequestBody NotificationRequest request) {

        Notification notification = new Notification();

        notification.setUserId(request.userId());
        notification.setTipo(request.tipo());
        notification.setMensaje(request.mensaje());

        return notificationService.crear(notification);
    }


    @GetMapping
    public List<Notification> obtenerTodas() {
        return notificationService.obtenerTodas();
    }

    @GetMapping("/{id}")
    public Notification obtenerPorId(@PathVariable Long id) {
        return notificationService.obtenerPorId(id);
    }

    @GetMapping("/user/{userId}")
    public List<Notification> obtenerPorUsuario(
            @PathVariable Long userId) {
        return notificationService.obtenerPorUsuario(userId);
    }


    @PutMapping("/{id}/status")
    public Notification actualizarEstado(
            @PathVariable Long id,
            @RequestBody StatusRequest request) {

        if (request == null || request.estado() == null) {
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

        return notificationService.actualizarEstado(id, estado);
    }


    public record StatusRequest(String estado) {}
}

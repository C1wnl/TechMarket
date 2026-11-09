package com.marketplace.notification.service;

import com.marketplace.notification.model.Notification;
import com.marketplace.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification crear(Notification notification) {
        return notificationRepository.save(notification);
    }

    public List<Notification> obtenerTodas() {
        return notificationRepository.findAll();
    }

    public Notification obtenerPorId(Long id, Long userId, boolean admin) {
        if (admin) {
            return notificationRepository.findById(id)
                    .orElseThrow(() -> notificacionNoEncontrada());
        }

        return notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> notificacionNoEncontrada());
    }

    public List<Notification> obtenerPorUsuario(
            Long userIdSolicitado,
            Long userIdAutenticado,
            boolean admin
    ) {
        if (!admin && !userIdSolicitado.equals(userIdAutenticado)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No puedes consultar las notificaciones de otro usuario"
            );
        }

        return notificationRepository.findByUserId(userIdSolicitado);
    }

    public Notification actualizarEstado(
            Long id,
            String estado,
            Long userId,
            boolean admin
    ) {
        Notification notification = obtenerPorId(id, userId, admin);
        notification.setEstado(estado);

        return notificationRepository.save(notification);
    }

    private ResponseStatusException notificacionNoEncontrada() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "La notificación no existe o no pertenece a tu cuenta"
        );
    }
}

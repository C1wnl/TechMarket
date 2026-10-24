
package com.marketplace.notification.service;

import com.marketplace.notification.model.Notification;
import com.marketplace.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

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

    public Notification obtenerPorId(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Notificación no encontrada con ID: " + id));
    }

    public List<Notification> obtenerPorUsuario(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public Notification actualizarEstado(Long id, String estado) {
        Notification notification = obtenerPorId(id);
        notification.setEstado(estado);
        return notificationRepository.save(notification);
    }
}

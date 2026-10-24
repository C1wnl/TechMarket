
package com.marketplace.payment.service;

import com.marketplace.payment.client.NotificationClient;
import com.marketplace.payment.model.Payment;
import com.marketplace.payment.repository.PaymentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PaymentService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PENDIENTE",
            "APROBADO",
            "RECHAZADO",
            "CANCELADO"
    );

    private final PaymentRepository paymentRepository;
    private final NotificationClient notificationClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            NotificationClient notificationClient) {
        this.paymentRepository = paymentRepository;
        this.notificationClient = notificationClient;
    }

    public Payment crearPago(Payment payment) {
        payment.setEstado("PENDIENTE");
        return paymentRepository.save(payment);
    }

    public List<Payment> obtenerPagos() {
        return paymentRepository.findAll(Sort.by("id"));
    }

    public Payment obtenerPagoPorId(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El pago no existe"
                ));
    }

    public List<Payment> obtenerPagosPorPedido(Long orderId) {
        return paymentRepository.findByOrderId(orderId, Sort.by("id"));
    }

    public List<Payment> obtenerPagosPorUsuario(Long userId) {
        return paymentRepository.findByUserId(userId, Sort.by("id"));
    }


    public Payment actualizarEstado(Long id, String estado) {

        if (estado == null || estado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El estado es obligatorio"
            );
        }

        String nuevoEstado = estado.trim().toUpperCase(Locale.ROOT);

        if (!ESTADOS_VALIDOS.contains(nuevoEstado)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado no válido. Use: PENDIENTE, APROBADO, RECHAZADO o CANCELADO"
            );
        }

        Payment payment = obtenerPagoPorId(id);
        String estadoActual = payment.getEstado();

        // Evitar actualizaciones y notificaciones duplicadas
        if (nuevoEstado.equals(estadoActual)) {
            return payment;
        }

        // No permitir cambios desde estados finales
        if (Set.of("APROBADO", "RECHAZADO", "CANCELADO")
                .contains(estadoActual)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede modificar un pago con estado final: "
                            + estadoActual
            );
        }

        // Solo permitir transiciones desde PENDIENTE
        if (!"PENDIENTE".equals(estadoActual)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transición de estado no permitida"
            );
        }

        payment.setEstado(nuevoEstado);

        Payment pagoActualizado = paymentRepository.save(payment);

        if ("APROBADO".equals(nuevoEstado)
                || "RECHAZADO".equals(nuevoEstado)) {

            notificationClient.notificarCambioDePago(pagoActualizado);
        }

        return pagoActualizado;
    }

}

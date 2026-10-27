
package com.marketplace.payment.service;

import com.marketplace.payment.model.Payment;
import com.marketplace.payment.repository.PaymentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.marketplace.payment.event.PaymentEvent;
import com.marketplace.payment.outbox.PaymentOutboxEvent;
import com.marketplace.payment.outbox.PaymentOutboxRepository;

import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;
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
    private final PaymentOutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentOutboxRepository outboxRepository,
            JsonMapper jsonMapper) {

        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
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



    @Transactional
    public Payment actualizarEstado(Long id, String estado) {

        if (estado == null || estado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El estado es obligatorio"
            );
        }

        String nuevoEstado =
                estado.trim().toUpperCase(Locale.ROOT);

        if (!ESTADOS_VALIDOS.contains(nuevoEstado)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado no válido"
            );
        }

        Payment payment = obtenerPagoPorId(id);
        String estadoActual = payment.getEstado();

        if (nuevoEstado.equals(estadoActual)) {
            return payment;
        }

        if (Set.of("APROBADO", "RECHAZADO", "CANCELADO")
                .contains(estadoActual)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede modificar un pago finalizado"
            );
        }

        if (!"PENDIENTE".equals(estadoActual)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transición no permitida"
            );
        }

        payment.setEstado(nuevoEstado);
        Payment pagoActualizado =
                paymentRepository.save(payment);

        if ("APROBADO".equals(nuevoEstado)
                || "RECHAZADO".equals(nuevoEstado)) {

            String eventType =
                    "APROBADO".equals(nuevoEstado)
                            ? "PAYMENT_APPROVED"
                            : "PAYMENT_REJECTED";

            String eventId = UUID.randomUUID().toString();

            PaymentEvent evento = new PaymentEvent(
                    eventId,
                    eventType,
                    pagoActualizado.getId(),
                    pagoActualizado.getOrderId(),
                    pagoActualizado.getUserId(),
                    pagoActualizado.getMonto(),
                    nuevoEstado,
                    Instant.now().toString()
            );

            String payload =
                    jsonMapper.writeValueAsString(evento);

            PaymentOutboxEvent outbox =
                    new PaymentOutboxEvent(
                            eventId,
                            pagoActualizado.getId(),
                            payload
                    );

            outboxRepository.save(outbox);
        }

        return pagoActualizado;
    }


}

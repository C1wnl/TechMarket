
package com.marketplace.payment.service;

import com.marketplace.payment.model.Payment;
import com.marketplace.payment.repository.PaymentRepository;
import com.marketplace.payment.event.PaymentEvent;
import com.marketplace.payment.outbox.PaymentOutboxEvent;
import com.marketplace.payment.outbox.PaymentOutboxRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Objects;

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

    // =====================================================
    // AUTORIZACIÓN Y PROPIEDAD
    // =====================================================

    private Authentication obtenerAutenticacion() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión"
            );
        }

        return authentication;
    }

    private boolean esAdmin(Authentication authentication) {

        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private Long obtenerUsuarioAutenticado() {

        Authentication authentication = obtenerAutenticacion();

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Identidad de usuario no válida"
            );
        }
    }

    private void verificarPropietario(Payment payment) {

        Authentication authentication = obtenerAutenticacion();

        if (esAdmin(authentication)) {
            return;
        }

        Long userId = obtenerUsuarioAutenticado();

        if (!Objects.equals(payment.getUserId(), userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para consultar este pago"
            );
        }
    }

    private void verificarAccesoUsuario(Long userId) {

        Authentication authentication = obtenerAutenticacion();

        if (esAdmin(authentication)) {
            return;
        }

        Long usuarioAutenticado = obtenerUsuarioAutenticado();

        if (!Objects.equals(usuarioAutenticado, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No puedes consultar los pagos de otro usuario"
            );
        }
    }

    private void verificarAdmin() {

        Authentication authentication = obtenerAutenticacion();

        if (!esAdmin(authentication)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Esta operación requiere permisos de administrador"
            );
        }
    }

    // =====================================================
    // CREACIÓN DE PAGOS
    // =====================================================

    public Payment crearPago(Payment payment) {

        // Sin transacción exterior: un INSERT concurrente puede
        // perder la carrera del UNIQUE. Su rollback debe terminar
        // antes de consultar el pago ganador.

        var existing = existingFor(payment);

        if (existing != null) {
            return existing;
        }

        payment.setEstado("PENDIENTE");

        try {
            return paymentRepository.saveAndFlush(payment);

        } catch (org.springframework.dao.DataIntegrityViolationException ex) {

            existing = existingFor(payment);

            if (existing != null) {
                return existing;
            }

            throw ex;
        }
    }

    private Payment existingFor(Payment request) {

        var matches = paymentRepository.findByOrderId(
                request.getOrderId(),
                Sort.by("id")
        );

        if (matches.isEmpty()) {
            return null;
        }

        if (matches.size() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Hay pagos históricos duplicados para este pedido; revisar manualmente"
            );
        }

        var payment = matches.getFirst();

        if (!Objects.equals(payment.getUserId(), request.getUserId())
                || payment.getMonto().compareTo(request.getMonto()) != 0
                || !Objects.equals(payment.getMetodoPago(), request.getMetodoPago())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pedido ya tiene un pago con otros datos"
            );
        }

        return payment;
    }

    // =====================================================
    // CONSULTAS
    // =====================================================

    public List<Payment> obtenerPagos() {

        // La consulta global está reservada para ADMIN.
        verificarAdmin();

        return paymentRepository.findAll(Sort.by("id"));
    }

    public Payment obtenerPagoPorId(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El pago no existe"
                ));

        verificarPropietario(payment);

        return payment;
    }

    public List<Payment> obtenerPagosPorPedido(Long orderId) {

        List<Payment> payments = paymentRepository.findByOrderId(
                orderId,
                Sort.by("id")
        );

        // Si existe un pago, verificamos su propietario.
        for (Payment payment : payments) {
            verificarPropietario(payment);
        }

        return payments;
    }

    public List<Payment> obtenerPagosPorUsuario(Long userId) {

        verificarAccesoUsuario(userId);

        return paymentRepository.findByUserId(
                userId,
                Sort.by("id")
        );
    }

    // =====================================================
    // ACTUALIZACIÓN DE ESTADO
    // =====================================================

    @Transactional
    public Payment actualizarEstado(Long id, String estado) {

        // Segunda protección: solamente ADMIN puede modificar estados.
        verificarAdmin();

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
                    "Estado no válido"
            );
        }

        Payment payment = paymentRepository.lockById(id).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El pago no existe"
                )
        );

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

        Payment pagoActualizado = paymentRepository.save(payment);

        String eventType = switch (nuevoEstado) {
            case "APROBADO" -> "PAYMENT_APPROVED";
            case "RECHAZADO" -> "PAYMENT_REJECTED";
            case "CANCELADO" -> "PAYMENT_CANCELLED";
            default -> null;
        };

        if (eventType != null) {
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

            String payload = jsonMapper.writeValueAsString(evento);

            PaymentOutboxEvent outbox = new PaymentOutboxEvent(
                    eventId,
                    pagoActualizado.getId(),
                    payload
            );

            outboxRepository.save(outbox);
        }

        return pagoActualizado;
    }
}


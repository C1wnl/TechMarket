package com.marketplace.payment.controller;

import com.marketplace.payment.dto.PaymentRequest;
import com.marketplace.payment.dto.PaymentStatusRequest;
import com.marketplace.payment.dto.PaymentResponse;
import com.marketplace.payment.dto.PaymentCreationResponse;
import com.marketplace.payment.model.Payment;
import com.marketplace.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INTERNAL')")
    public PaymentCreationResponse crearPago(
            @Valid @RequestBody PaymentRequest request) {

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setUserId(request.getUserId());
        payment.setMonto(request.getMonto());
        payment.setMetodoPago(request.getMetodoPago().trim());

        return toCreationResponse(paymentService.crearPago(payment));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<PaymentResponse> obtenerPagos() {
        return paymentService.obtenerPagos()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PaymentResponse obtenerPagoPorId(@PathVariable Long id) {
        return toResponse(paymentService.obtenerPagoPorId(id));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public List<PaymentResponse> obtenerPagosPorPedido(
            @PathVariable Long orderId) {

        return paymentService.obtenerPagosPorPedido(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public List<PaymentResponse> obtenerPagosPorUsuario(
            @PathVariable Long userId) {

        return paymentService.obtenerPagosPorUsuario(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody PaymentStatusRequest request) {

        return toResponse(
                paymentService.actualizarEstado(id, request.getEstado())
        );
    }

    private PaymentCreationResponse toCreationResponse(Payment payment) {
        return new PaymentCreationResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getMonto(),
                payment.getMetodoPago(),
                payment.getEstado()
        );
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getMonto(),
                payment.getMetodoPago(),
                payment.getEstado(),
                payment.getFechaCreacion(),
                payment.getFechaActualizacion()
        );
    }
}

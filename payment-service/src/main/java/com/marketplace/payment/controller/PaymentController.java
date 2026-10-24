package com.marketplace.payment.controller;

import com.marketplace.payment.dto.PaymentRequest;
import com.marketplace.payment.dto.PaymentStatusRequest;
import com.marketplace.payment.model.Payment;
import com.marketplace.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public Payment crearPago(@Valid @RequestBody PaymentRequest request) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setUserId(request.getUserId());
        payment.setMonto(request.getMonto());
        payment.setMetodoPago(request.getMetodoPago().trim());

        return paymentService.crearPago(payment);
    }

    @GetMapping
    public List<Payment> obtenerPagos() {
        return paymentService.obtenerPagos();
    }

    @GetMapping("/{id}")
    public Payment obtenerPagoPorId(@PathVariable Long id) {
        return paymentService.obtenerPagoPorId(id);
    }

    @GetMapping("/order/{orderId}")
    public List<Payment> obtenerPagosPorPedido(@PathVariable Long orderId) {
        return paymentService.obtenerPagosPorPedido(orderId);
    }

    @GetMapping("/user/{userId}")
    public List<Payment> obtenerPagosPorUsuario(@PathVariable Long userId) {
        return paymentService.obtenerPagosPorUsuario(userId);
    }

    @PutMapping("/{id}/status")
    public Payment actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody PaymentStatusRequest request) {
        return paymentService.actualizarEstado(id, request.getEstado());
    }
}

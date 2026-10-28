package com.marketplace.product.controller;

import com.marketplace.product.dto.StockRequest;
import com.marketplace.product.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/reservations")
public class ReservationController {
    private final ReservationService reservations;
    private final String secret;
    public ReservationController(ReservationService reservations, @Value("${stock.internal.key}") String secret) {
        this.reservations = reservations; this.secret = secret;
    }
    private void authorize(String supplied) {
        if (supplied == null || !MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clave interna inválida");
    }
    @PostMapping("/{orderId}/products/{productId}/reserve")
    public ResponseEntity<Void> reserve(@PathVariable Long orderId, @PathVariable Long productId,
            @Valid @RequestBody StockRequest request, @RequestHeader(value="X-Internal-Key", required=false) String key) {
        authorize(key); reservations.reserve(orderId, productId, request.quantity());
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{orderId}/products/{productId}/release")
    public ResponseEntity<Void> release(@PathVariable Long orderId, @PathVariable Long productId,
            @RequestHeader(value="X-Internal-Key", required=false) String key) {
        authorize(key); reservations.release(orderId, productId);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{orderId}/products/{productId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable Long orderId, @PathVariable Long productId,
            @RequestHeader(value="X-Internal-Key", required=false) String key) {
        authorize(key); reservations.confirm(orderId, productId);
        return ResponseEntity.noContent().build();
    }
}

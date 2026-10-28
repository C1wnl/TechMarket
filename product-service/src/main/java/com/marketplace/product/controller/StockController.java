package com.marketplace.product.controller;

import com.marketplace.product.dto.StockRequest;
import com.marketplace.product.service.StockService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/products")
public class StockController {
    private final StockService stock;
    private final String key;

    public StockController(StockService stock, @Value("${stock.internal.key}") String key) {
        this.stock = stock;
        this.key = key;
    }

    private void validarClave(String supplied) {
        if (supplied == null || !MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clave interna inválida");
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<Void> reservar(@PathVariable Long id, @Valid @RequestBody StockRequest request,
            @RequestHeader(value = "X-Internal-Key", required = false) String key) {
        validarClave(key);
        stock.reservar(id, request.quantity());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> liberar(@PathVariable Long id, @Valid @RequestBody StockRequest request,
            @RequestHeader(value = "X-Internal-Key", required = false) String key) {
        validarClave(key);
        stock.liberar(id, request.quantity());
        return ResponseEntity.noContent().build();
    }
}

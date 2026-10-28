package com.marketplace.product.service;

import com.marketplace.product.model.StockReservation;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.product.repository.StockReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationService {
    private final ProductRepository products;
    private final StockReservationRepository reservations;
    public ReservationService(ProductRepository products, StockReservationRepository reservations) {
        this.products = products; this.reservations = reservations;
    }

    @Transactional
    public void reserve(Long orderId, Long productId, Integer quantity) {
        validate(orderId, productId, quantity);
        var product = products.lockById(productId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto inexistente"));
        var existing = reservations.findByOrderIdAndProductId(orderId, productId);
        if (existing.isPresent()) {
            var r = existing.get();
            if (r.getQuantity().equals(quantity) && "RESERVED".equals(r.getStatus())) return;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserva ya utilizada o con cantidad diferente");
        }
        if (product.getStock() < quantity)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente");
        product.setStock(product.getStock() - quantity);
        reservations.save(new StockReservation(orderId, productId, quantity));
    }

    @Transactional
    public void release(Long orderId, Long productId) {
        requireIds(orderId, productId);
        // La misma fila del producto serializa reserve/release/confirm.
        products.lockById(productId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto inexistente"));
        var existing = reservations.findByOrderIdAndProductId(orderId, productId);
        if (existing.isEmpty()) {
            var tombstone = new StockReservation(orderId, productId, 0);
            tombstone.setStatus("RELEASED");
            reservations.save(tombstone);
            return;
        }
        var reservation = existing.get();
        if ("RELEASED".equals(reservation.getStatus())) return;
        if (!"RESERVED".equals(reservation.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserva confirmada, no liberable");
        // La operación de stock y el estado de reserva se confirman juntos.
        var product = products.lockById(productId).orElseThrow();
        product.setStock(product.getStock() + reservation.getQuantity());
        reservation.setStatus("RELEASED");
    }

    @Transactional
    public void confirm(Long orderId, Long productId) {
        requireIds(orderId, productId);
        products.lockById(productId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto inexistente"));
        var reservation = reservations.findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva inexistente"));
        if ("CONFIRMED".equals(reservation.getStatus())) return;
        if (!"RESERVED".equals(reservation.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserva liberada, no confirmable");
        reservation.setStatus("CONFIRMED");
    }

    private void requireIds(Long orderId, Long productId) {
        if (orderId == null || orderId <= 0 || productId == null || productId <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IDs inválidos");
    }
    private void validate(Long orderId, Long productId, Integer quantity) {
        requireIds(orderId, productId);
        if (quantity == null || quantity <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad inválida");
    }
}

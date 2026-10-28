package com.marketplace.product.service;

import com.marketplace.product.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StockService {
    private final ProductRepository products;

    public StockService(ProductRepository products) { this.products = products; }

    @Transactional
    public void reservar(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad inválida");
        if (products.reservar(productId, quantity) == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Producto inexistente o stock insuficiente");
    }

    @Transactional
    public void liberar(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad inválida");
        if (products.liberar(productId, quantity) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto inexistente");
    }
}

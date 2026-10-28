package com.marketplace.order.controller;

import com.marketplace.order.dto.OrderRequest;
import com.marketplace.order.model.Order;
import com.marketplace.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order crearPedido(@Valid @RequestBody OrderRequest request, Authentication auth,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return orderService.crearPedido(Long.valueOf(auth.getName()), request, key);
    }

    @GetMapping
    public List<Order> obtenerPedidos(Authentication auth) {
        return orderService.obtenerPedidos(Long.valueOf(auth.getName()), isAdmin(auth));
    }

    @GetMapping("/{id}")
    public Order obtenerPedidoPorId(@PathVariable Long id, Authentication auth) {
        return orderService.obtenerPedidoPorId(id, Long.valueOf(auth.getName()), isAdmin(auth));
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));
    }
}

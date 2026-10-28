package com.marketplace.order.controller;

import com.marketplace.order.model.Order;
import com.marketplace.order.service.OrderService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// SOLO LABORATORIO. No desplegar el perfil local en un entorno público.
@RestController
@Profile("local")
@RequestMapping("/internal/demo/orders")
public class LocalSagaDemoController {
    private final OrderService orders;

    public LocalSagaDemoController(OrderService orders) { this.orders = orders; }

    @PostMapping("/{id}/request-payment")
    public Order requestPayment(@PathVariable Long id) { return orders.solicitarPago(id); }

    @PostMapping("/{id}/approve")
    public Order approve(@PathVariable Long id) { return orders.aprobarPago(id); }

    @PostMapping("/{id}/reject")
    public Order reject(@PathVariable Long id) { return orders.rechazarPago(id); }
}


package com.marketplace.order.controller;

import com.marketplace.order.client.UserClient;
import com.marketplace.order.dto.AdminOrderResponse;
import com.marketplace.order.dto.InternalUserResponse;
import com.marketplace.order.dto.OrderItemResponse;
import com.marketplace.order.dto.OrderRequest;
import com.marketplace.order.dto.OrderResponse;
import com.marketplace.order.model.Order;
import com.marketplace.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserClient userClient;

    public OrderController(
            OrderService orderService,
            UserClient userClient
    ) {
        this.orderService = orderService;
        this.userClient = userClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse crearPedido(
            @Valid @RequestBody OrderRequest request,
            Authentication auth,
            @RequestHeader(value = "Idempotency-Key", required = false) String key
    ) {
        Order order = orderService.crearPedido(
                Long.valueOf(auth.getName()), request, key
        );

        return convertirRespuesta(order);
    }

    @GetMapping
    public List<OrderResponse> obtenerPedidos(Authentication auth) {

        List<Order> pedidos = orderService.obtenerPedidos(
                Long.valueOf(auth.getName()), isAdmin(auth)
        );

        return pedidos.stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    // Consulta administrativa con datos del comprador.
    @GetMapping("/admin")
    public List<AdminOrderResponse> obtenerPedidosAdmin(Authentication auth) {

        if (!isAdmin(auth)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo ADMIN puede consultar todos los pedidos"
            );
        }

        List<Order> pedidos = orderService.obtenerPedidos(
                Long.valueOf(auth.getName()), true
        );

        // Evita consultar User Service varias veces para el mismo comprador.
        Map<Long, InternalUserResponse> usuarios = new HashMap<>();

        return pedidos.stream()
                .map(order -> {
                    InternalUserResponse usuario = usuarios.computeIfAbsent(
                            order.getUserId(),
                            userClient::obtenerUsuario
                    );

                    List<OrderItemResponse> items = convertirItems(order);

                    return new AdminOrderResponse(
                            order.getId(),
                            usuario.nombre(),
                            usuario.email(),
                            order.getEstado(),
                            order.getTotal(),
                            order.getFechaCreacion(),
                            items
                    );
                })
                .toList();
    }

    @GetMapping("/{id}")
    public OrderResponse obtenerPedidoPorId(
            @PathVariable Long id,
            Authentication auth
    ) {
        Order order = orderService.obtenerPedidoPorId(
                id,
                Long.valueOf(auth.getName()),
                isAdmin(auth)
        );

        return convertirRespuesta(order);
    }

    private OrderResponse convertirRespuesta(Order order) {

        return new OrderResponse(
                order.getId(),
                order.getEstado(),
                order.getTotal(),
                order.getFechaCreacion(),
                convertirItems(order)
        );
    }

    private List<OrderItemResponse> convertirItems(Order order) {

        return order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getNombre(),
                        item.getPrecioUnitario(),
                        item.getCantidad(),
                        item.getSubtotal()
                ))
                .toList();
    }

    private boolean isAdmin(Authentication auth) {

        return auth != null
                && auth.getAuthorities().stream()
                .anyMatch(role ->
                        role.getAuthority().equals("ROLE_ADMIN")
                );
    }
}

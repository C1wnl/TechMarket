package com.marketplace.order.service;

import com.marketplace.order.client.ProductClient;
import com.marketplace.order.dto.OrderRequest;
import com.marketplace.order.model.*;
import com.marketplace.order.repository.OrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final com.marketplace.order.payment.PaymentWorkflow workflow;
    public OrderService(OrderRepository orderRepository, ProductClient productClient,
            org.springframework.beans.factory.ObjectProvider<com.marketplace.order.payment.PaymentWorkflow> workflow) {
        this.orderRepository = orderRepository; this.productClient = productClient;
        this.workflow = workflow.getIfAvailable();
    }
    public Order crearPedido(Long userId, OrderRequest request) {
        return crearPedido(userId, request, null);
    }
    public Order crearPedido(Long userId, OrderRequest request, String idempotencyKey) {
        String key = workflow == null ? null : workflow.key(userId, idempotencyKey);
        if (key != null) {
            var existing = workflow.existing(key, request);
            if (existing != null) return existing;
        }
        var seen = new HashSet<Long>();
        for (var item : request.items())
            if (!seen.add(item.productId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No repitas un producto; reúne sus unidades en una sola línea");
        var items = new ArrayList<OrderItem>();
        BigDecimal total = BigDecimal.ZERO;
        for (var requested : request.items()) {
            var product = productClient.obtenerProducto(requested.productId());
            if (requested.cantidad() > product.stock())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente para " + product.nombre());
            BigDecimal subtotal = product.precio().multiply(BigDecimal.valueOf(requested.cantidad()));
            total = total.add(subtotal);
            if (total.compareTo(new BigDecimal("9999999999.99")) > 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El total supera el máximo permitido");
            items.add(new OrderItem(product.id(), product.nombre(), product.precio(), requested.cantidad(),
                    subtotal.setScale(2, RoundingMode.UNNECESSARY)));
        }
        Order order = new Order();
        order.setUserId(userId); order.setItems(items);
        order.setTotal(total.setScale(2, RoundingMode.UNNECESSARY));
        order.setStatus(OrderStatus.PENDIENTE);
        if (workflow != null) {
            order.setRequestKey(key);
            return workflow.start(order, request);
        }
        // Se necesita un ID persistido antes de reservar para idempotencia.
        order = orderRepository.saveAndFlush(order);
        try {
            for (var item : items) productClient.reservar(order.getId(), item.getProductId(), item.getCantidad());
            // Etapa actual: PENDIENTE. PENDIENTE_PAGO se activará con el flujo de Kafka.
            return order;
        } catch (RuntimeException error) {
            boolean released = true;
            // Intentamos liberar TODOS: una respuesta de reserva pudo perderse tras ejecutarse.
            for (var item : items) {
                try { productClient.liberar(order.getId(), item.getProductId()); }
                catch (RuntimeException failure) { released = false; error.addSuppressed(failure); }
            }
            order.setStatus(released ? OrderStatus.CANCELADO : OrderStatus.COMPENSACION_PENDIENTE);
            orderRepository.save(order);
            throw error;
        }
    }
    // Estos métodos son reutilizables cuando se conecte Kafka; el controlador demo es solo local.
    public Order solicitarPago(Long orderId) {
        Order order = buscar(orderId);
        if (order.getIntegrationPhase() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pedido integrado: resolver el pago en Payment y esperar Kafka");
        if (order.getStatus() == OrderStatus.PENDIENTE_PAGO) return order;
        exigirEstado(order, OrderStatus.PENDIENTE);
        order.setStatus(OrderStatus.PENDIENTE_PAGO);
        return orderRepository.save(order);
    }

    public Order aprobarPago(Long orderId) {
        Order order = buscar(orderId);
        if (order.getIntegrationPhase() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pedido integrado: resolver el pago en Payment y esperar Kafka");
        if (order.getStatus() == OrderStatus.CONFIRMADO) return order;
        exigirEstado(order, OrderStatus.PENDIENTE_PAGO);
        // Product confirma cada reserva de modo idempotente: un fallo permite reintentar.
        // No marcar CONFIRMADO si alguna confirmación no respondió satisfactoriamente.
        for (var item : order.getItems()) {
            productClient.confirmar(order.getId(), item.getProductId());
        }
        order.setStatus(OrderStatus.CONFIRMADO);
        return orderRepository.save(order);
    }

    public Order rechazarPago(Long orderId) {
        Order order = buscar(orderId);
        if (order.getIntegrationPhase() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pedido integrado: resolver el pago en Payment y esperar Kafka");
        if (order.getStatus() == OrderStatus.CANCELADO) return order;
        if (order.getStatus() != OrderStatus.PENDIENTE_PAGO &&
                order.getStatus() != OrderStatus.COMPENSACION_PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede rechazar en este estado");
        }
        // Registrar el estado de recuperación antes de llamadas HTTP externas.
        order.setStatus(OrderStatus.COMPENSACION_PENDIENTE);
        orderRepository.saveAndFlush(order);
        boolean allReleased = true;
        for (var item : order.getItems()) {
            try { productClient.liberar(order.getId(), item.getProductId()); }
            catch (RuntimeException ex) { allReleased = false; }
        }
        if (!allReleased) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Compensación incompleta; reintenta el rechazo del pedido");
        }
        order.setStatus(OrderStatus.CANCELADO);
        return orderRepository.save(order);
    }

    private Order buscar(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido inexistente"));
    }

    private void exigirEstado(Order order, OrderStatus requerido) {
        if (order.getStatus() != requerido) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transición inválida desde " + order.getStatus());
        }
    }

    public List<Order> obtenerPedidos(Long userId, boolean admin) {
        return admin ? orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                : orderRepository.findByUserIdOrderByIdDesc(userId);
    }
    public Order obtenerPedidoPorId(Long id, Long userId, boolean admin) {
        return orderRepository.findById(id).filter(o -> admin || o.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El pedido no existe o no pertenece a tu cuenta"));
    }
}

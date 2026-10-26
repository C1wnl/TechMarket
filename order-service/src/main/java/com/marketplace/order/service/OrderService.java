package com.marketplace.order.service;

import com.marketplace.order.client.ProductClient;
import com.marketplace.order.dto.OrderRequest;
import com.marketplace.order.model.Order;
import com.marketplace.order.model.OrderItem;
import com.marketplace.order.repository.OrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    public Order crearPedido(Long userId, OrderRequest request) {
        var seen = new HashSet<Long>();
        for (var item : request.items()) {
            if (!seen.add(item.productId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No repitas un producto; reúne sus unidades en una sola línea");
            }
        }
        var items = new ArrayList<OrderItem>();
        BigDecimal total = BigDecimal.ZERO;
        for (var requested : request.items()) {
            var product = productClient.obtenerProducto(requested.productId());
            if (requested.cantidad() > product.stock()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock insuficiente para " + product.nombre());
            }
            BigDecimal subtotal = product.precio().multiply(BigDecimal.valueOf(requested.cantidad()));
            total = total.add(subtotal);
            if (total.compareTo(new BigDecimal("9999999999.99")) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El total supera el máximo permitido");
            }
            items.add(new OrderItem(product.id(), product.nombre(), product.precio(),
                    requested.cantidad(), subtotal.setScale(2, RoundingMode.UNNECESSARY)));
        }
        Order order = new Order();
        order.setUserId(userId);
        order.setItems(items);
        order.setTotal(total.setScale(2, RoundingMode.UNNECESSARY));
        order.setEstado("PENDIENTE");
        var reservados = new ArrayList<OrderItem>();
        try {
            for (var item : items) {
                productClient.reservar(item.getProductId(), item.getCantidad());
                reservados.add(item);
            }
            return orderRepository.save(order);
        } catch (RuntimeException error) {
            // Compensación ante fallo de otra reserva o del guardado local.
            for (int i = reservados.size() - 1; i >= 0; i--) {
                var item = reservados.get(i);
                try {
                    productClient.liberar(item.getProductId(), item.getCantidad());
                } catch (RuntimeException compensationFailure) {
                    error.addSuppressed(compensationFailure);
                }
            }
            throw error;
        }
    }

    public List<Order> obtenerPedidos(Long userId, boolean admin) {
        return admin ? orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                : orderRepository.findByUserIdOrderByIdDesc(userId);
    }

    public Order obtenerPedidoPorId(Long id, Long userId, boolean admin) {
        return orderRepository.findById(id)
                .filter(order -> admin || order.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El pedido no existe o no pertenece a tu cuenta"));
    }
}

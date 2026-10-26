package com.marketplace.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class OrderItem {
    @Column(nullable = false)
    private Long productId;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;
    @Column(nullable = false)
    private Integer cantidad;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    public OrderItem() { }

    public OrderItem(Long productId, String nombre, BigDecimal precioUnitario,
                     Integer cantidad, BigDecimal subtotal) {
        this.productId = productId;
        this.nombre = nombre;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.subtotal = subtotal;
    }

    public Long getProductId() { return productId; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getSubtotal() { return subtotal; }
}

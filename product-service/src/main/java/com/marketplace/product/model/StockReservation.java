package com.marketplace.product.model;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_reservations", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "product_id"}))
public class StockReservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(nullable = false)
    private Integer quantity;
    @Column(nullable = false, length = 20)
    private String status;
    protected StockReservation() { }
    public StockReservation(Long orderId, Long productId, Integer quantity) {
        this.orderId = orderId; this.productId = productId; this.quantity = quantity; this.status = "RESERVED";
    }
    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

package com.marketplace.order.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus estado = OrderStatus.PENDIENTE;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_number")
    private List<OrderItem> items = new ArrayList<>();
    // Nullable para conservar los pedidos históricos y el modo de simulación.
    @Column(unique = true)
    private Long paymentId;
    @Column(unique = true, length = 200)
    private String requestKey;
    private String integrationPhase;
    private String paymentOutcome;
    private Integer integrationAttempts = 0;
    private java.time.Instant nextAttempt;
    @Column(length = 1000)
    private String lastIntegrationError;
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long value) { paymentId = value; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String value) { requestKey = value; }
    public String getIntegrationPhase() { return integrationPhase; }
    public void setIntegrationPhase(String value) { integrationPhase = value; }
    public String getPaymentOutcome() { return paymentOutcome; }
    public void setPaymentOutcome(String value) { paymentOutcome = value; }
    public Integer getIntegrationAttempts() { return integrationAttempts; }
    public void setIntegrationAttempts(Integer value) { integrationAttempts = value; }
    public java.time.Instant getNextAttempt() { return nextAttempt; }
    public void setNextAttempt(java.time.Instant value) { nextAttempt = value; }
    public String getLastIntegrationError() { return lastIntegrationError; }
    public void setLastIntegrationError(String value) { lastIntegrationError = value; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getEstado() { return estado.name(); }
    public OrderStatus getStatus() { return estado; }
    public void setEstado(String value) { estado = OrderStatus.valueOf(value); }
    public void setStatus(OrderStatus status) { this.estado = status; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}

package com.agencias.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ORDER_ITEM")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_item_seq")
    @SequenceGenerator(name = "order_item_seq", sequenceName = "ORDER_ITEM_SEQ", allocationSize = 1)
    @Column(name = "ORDER_ITEM_ID")
    private Long orderItemId;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    @Column(name = "PART_ID", nullable = false)
    private Long partId;

    @Column(nullable = false)
    private Integer qty;

    @Column(name = "UNIT_PRICE", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "LINE_TOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    public OrderItem() {
    }

    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}

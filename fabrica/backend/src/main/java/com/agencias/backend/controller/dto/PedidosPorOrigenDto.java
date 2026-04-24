package com.agencias.backend.controller.dto;

import java.math.BigDecimal;

/** Cantidad e importe de pedidos agrupados por canal (tienda fábrica vs distribuidora). */
public class PedidosPorOrigenDto {
    private String orderOrigin;
    private long pedidoCount;
    private BigDecimal totalImporte;

    public String getOrderOrigin() { return orderOrigin; }
    public void setOrderOrigin(String orderOrigin) { this.orderOrigin = orderOrigin; }
    public long getPedidoCount() { return pedidoCount; }
    public void setPedidoCount(long pedidoCount) { this.pedidoCount = pedidoCount; }
    public BigDecimal getTotalImporte() { return totalImporte; }
    public void setTotalImporte(BigDecimal totalImporte) { this.totalImporte = totalImporte; }
}

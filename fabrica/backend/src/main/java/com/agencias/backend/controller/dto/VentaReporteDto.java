package com.agencias.backend.controller.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/** Una fila del reporte de ventas (por día o por pedido). */
public class VentaReporteDto {
    private Date fecha;
    private String orderNumber;
    private Long orderId;
    private Long userId;
    private String userDisplayName;
    private String orderType;
    private BigDecimal total;
    private List<VentaLineaDto> lineas;

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserDisplayName() { return userDisplayName; }
    public void setUserDisplayName(String userDisplayName) { this.userDisplayName = userDisplayName; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public List<VentaLineaDto> getLineas() { return lineas; }
    public void setLineas(List<VentaLineaDto> lineas) { this.lineas = lineas; }
}

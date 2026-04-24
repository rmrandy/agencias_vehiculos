package com.agencias.backend.controller.dto;

import java.math.BigDecimal;
import java.util.Date;

/** Un día con cantidad de pedidos e ingreso total (pedidos no cancelados). */
public class VentaDiariaDto {
    private Date fecha;
    private long pedidoCount;
    private BigDecimal totalImporte;

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    public long getPedidoCount() { return pedidoCount; }
    public void setPedidoCount(long pedidoCount) { this.pedidoCount = pedidoCount; }
    public BigDecimal getTotalImporte() { return totalImporte; }
    public void setTotalImporte(BigDecimal totalImporte) { this.totalImporte = totalImporte; }
}

package com.agencias.backend.controller.dto;

import java.math.BigDecimal;

/** Fila del reporte de repuestos más vendidos (unidades y monto en el período). */
public class MasVendidoDto {
    private Long partId;
    private String partNumber;
    private String partTitle;
    private long totalQty;
    private BigDecimal totalImporte;

    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public String getPartTitle() { return partTitle; }
    public void setPartTitle(String partTitle) { this.partTitle = partTitle; }
    public long getTotalQty() { return totalQty; }
    public void setTotalQty(long totalQty) { this.totalQty = totalQty; }
    public BigDecimal getTotalImporte() { return totalImporte; }
    public void setTotalImporte(BigDecimal totalImporte) { this.totalImporte = totalImporte; }
}

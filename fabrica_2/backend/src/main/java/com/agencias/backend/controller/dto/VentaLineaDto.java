package com.agencias.backend.controller.dto;

import java.math.BigDecimal;

public class VentaLineaDto {
    private Long partId;
    private String partNumber;
    private String partTitle;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public String getPartTitle() { return partTitle; }
    public void setPartTitle(String partTitle) { this.partTitle = partTitle; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}

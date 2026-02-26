package com.agencias.backend.controller.dto;

import java.util.Date;

/** Fila de reporte: repuesto consultado / visto / agregado al carrito (agregado por partId). */
public class EngagementReporteDto {
    private Long partId;
    private String partNumber;
    private String partTitle;
    private Long count;
    private Date fromDate;
    private Date toDate;

    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public String getPartTitle() { return partTitle; }
    public void setPartTitle(String partTitle) { this.partTitle = partTitle; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
    public Date getFromDate() { return fromDate; }
    public void setFromDate(Date fromDate) { this.fromDate = fromDate; }
    public Date getToDate() { return toDate; }
    public void setToDate(Date toDate) { this.toDate = toDate; }
}

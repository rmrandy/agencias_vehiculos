package com.agencias.backend.controller.dto;

import java.util.Date;

/**
 * Log de alta de inventario para reportería.
 */
public class InventoryLogDto {
    private Long logId;
    private Long partId;
    private String partTitle;
    private String partNumber;
    private Long userId;
    private String userDisplayName;
    private Date createdAt;
    private Integer quantityAdded;
    private Integer previousQuantity;
    private Integer newQuantity;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }
    public String getPartTitle() { return partTitle; }
    public void setPartTitle(String partTitle) { this.partTitle = partTitle; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserDisplayName() { return userDisplayName; }
    public void setUserDisplayName(String userDisplayName) { this.userDisplayName = userDisplayName; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Integer getQuantityAdded() { return quantityAdded; }
    public void setQuantityAdded(Integer quantityAdded) { this.quantityAdded = quantityAdded; }
    public Integer getPreviousQuantity() { return previousQuantity; }
    public void setPreviousQuantity(Integer previousQuantity) { this.previousQuantity = previousQuantity; }
    public Integer getNewQuantity() { return newQuantity; }
    public void setNewQuantity(Integer newQuantity) { this.newQuantity = newQuantity; }
}

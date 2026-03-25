package com.agencias.backend.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Registro de cada alta de inventario en un repuesto.
 */
@Entity
@Table(name = "INVENTORY_LOG")
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_log_seq")
    @SequenceGenerator(name = "inventory_log_seq", sequenceName = "INVENTORY_LOG_SEQ", allocationSize = 1)
    @Column(name = "LOG_ID")
    private Long logId;

    @Column(name = "PART_ID", nullable = false)
    private Long partId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "QUANTITY_ADDED", nullable = false)
    private Integer quantityAdded;

    @Column(name = "PREVIOUS_QUANTITY", nullable = false)
    private Integer previousQuantity;

    @Column(name = "NEW_QUANTITY", nullable = false)
    private Integer newQuantity;

    @Column(name = "CREATED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = new Date();
    }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getQuantityAdded() { return quantityAdded; }
    public void setQuantityAdded(Integer quantityAdded) { this.quantityAdded = quantityAdded; }
    public Integer getPreviousQuantity() { return previousQuantity; }
    public void setPreviousQuantity(Integer previousQuantity) { this.previousQuantity = previousQuantity; }
    public Integer getNewQuantity() { return newQuantity; }
    public void setNewQuantity(Integer newQuantity) { this.newQuantity = newQuantity; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}

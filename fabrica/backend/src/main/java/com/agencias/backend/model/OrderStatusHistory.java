package com.agencias.backend.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "ORDER_STATUS_HISTORY")
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "status_seq")
    @SequenceGenerator(name = "status_seq", sequenceName = "STATUS_SEQ", allocationSize = 1)
    @Column(name = "STATUS_ID")
    private Long statusId;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "COMMENT_TEXT", length = 500)
    private String commentText;

    @Column(name = "TRACKING_NUMBER", length = 100)
    private String trackingNumber;

    @Column(name = "ETA_DAYS")
    private Integer etaDays;

    @Column(name = "CHANGED_BY_USER_ID")
    private Long changedByUserId;

    @Column(name = "CHANGED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date changedAt;

    @PrePersist
    public void prePersist() {
        if (changedAt == null) {
            changedAt = new Date();
        }
    }

    public OrderStatusHistory() {
    }

    public Long getStatusId() { return statusId; }
    public void setStatusId(Long statusId) { this.statusId = statusId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public Integer getEtaDays() { return etaDays; }
    public void setEtaDays(Integer etaDays) { this.etaDays = etaDays; }
    public Long getChangedByUserId() { return changedByUserId; }
    public void setChangedByUserId(Long changedByUserId) { this.changedByUserId = changedByUserId; }
    public Date getChangedAt() { return changedAt; }
    public void setChangedAt(Date changedAt) { this.changedAt = changedAt; }
}

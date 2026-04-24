package com.agencias.backend.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Eventos de engagement para reportería: repuesto visto en detalle, agregado al carrito, consultado (búsqueda).
 * Los distribuidores envían estos eventos a la fábrica.
 */
@Entity
@Table(name = "PART_ENGAGEMENT_LOG")
public class PartEngagementLog {

    public static final String EVENT_VIEW_DETAIL = "VIEW_DETAIL";
    public static final String EVENT_ADD_TO_CART = "ADD_TO_CART";
    public static final String EVENT_SEARCH = "SEARCH";

    public static final String CLIENT_PARTICULAR = "PARTICULAR";
    public static final String CLIENT_ENTERPRISE = "ENTERPRISE";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "engagement_log_seq")
    @SequenceGenerator(name = "engagement_log_seq", sequenceName = "PART_ENGAGEMENT_LOG_SEQ", allocationSize = 1)
    @Column(name = "LOG_ID")
    private Long logId;

    @Column(name = "EVENT_TYPE", nullable = false, length = 30)
    private String eventType;

    @Column(name = "PART_ID", nullable = false)
    private Long partId;

    @Column(name = "USER_ID")
    private Long userId;

    /** PARTICULAR o ENTERPRISE */
    @Column(name = "CLIENT_TYPE", length = 20)
    private String clientType;

    /** Identificador del distribuidor que envió el evento (opcional) */
    @Column(name = "SOURCE", length = 255)
    private String source;

    @Column(name = "CREATED_AT", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = new Date();
    }

    public PartEngagementLog() {
    }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}

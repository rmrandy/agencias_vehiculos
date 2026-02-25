package com.agencias.backend.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Log de operaciones de importación/exportación.
 * Quién, cuándo, archivo usado, cuántos exitosos, cuántos con error.
 */
@Entity
@Table(name = "IMPORT_EXPORT_LOG")
public class ImportExportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "import_export_log_seq")
    @SequenceGenerator(name = "import_export_log_seq", sequenceName = "IMPORT_EXPORT_LOG_SEQ", allocationSize = 1)
    @Column(name = "LOG_ID")
    private Long logId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    /** EXPORT, IMPORT, IMPORT_INVENTORY */
    @Column(name = "OPERATION", nullable = false, length = 50)
    private String operation;

    @Column(name = "FILE_NAME", length = 500)
    private String fileName;

    @Column(name = "SUCCESS_COUNT", nullable = false)
    private Integer successCount = 0;

    @Column(name = "ERROR_COUNT", nullable = false)
    private Integer errorCount = 0;

    @Lob
    @Column(name = "DETAIL")
    private String detail;

    @Column(name = "CREATED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = new Date();
    }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}

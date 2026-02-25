package com.agencias.backend.controller.dto;

import java.util.Date;

/**
 * Log de operación de import/export para reportería.
 * Quién, cuándo, archivo usado, cuántos exitosos, cuántos con error.
 */
public class ImportExportLogDto {
    private Long logId;
    private Long userId;
    private String userDisplayName;
    private Date createdAt;
    private String operation;
    private String fileName;
    private Integer successCount;
    private Integer errorCount;
    private String detail;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserDisplayName() { return userDisplayName; }
    public void setUserDisplayName(String userDisplayName) { this.userDisplayName = userDisplayName; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
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
}

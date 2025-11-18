package com.checkstylehub.analyzer.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for analysis request status information.
 * Used to provide status updates to the client (PENDING/CLONING/ANALYZING/COMPLETED/FAILED).
 */
public class AnalysisRequestStatusDto {
    private Long id;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;

    public AnalysisRequestStatusDto() {
    }

    public AnalysisRequestStatusDto(Long id, String status, String errorMessage, LocalDateTime createdAt) {
        this.id = id;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

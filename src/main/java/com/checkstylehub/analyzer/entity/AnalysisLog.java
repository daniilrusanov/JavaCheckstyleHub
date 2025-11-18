package com.checkstylehub.analyzer.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing a log entry for an analysis request.
 * Logs are persisted to a database and also sent via WebSocket in real-time.
 */
@Entity
@Table(name = "analysis_logs", indexes = {
        @Index(name = "idx_analysis_logs_request_id", columnList = "request_id")
})
public class AnalysisLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private AnalysisRequest request;

    @Column(nullable = false, length = 16)
    private String level;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AnalysisRequest getRequest() {
        return request;
    }

    public void setRequest(AnalysisRequest request) {
        this.request = request;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

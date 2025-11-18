package com.checkstylehub.analyzer.dto;

/**
 * Data Transfer Object for a single Checkstyle violation.
 * Contains a file path, line number, severity level, and violation message.
 */
public class AnalysisResultDto {
    private Long id;
    private String filePath;
    private int lineNumber;
    private String severity;
    private String message;

    public AnalysisResultDto() {
    }

    public AnalysisResultDto(Long id, String filePath, int lineNumber, String severity, String message) {
        this.id = id;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.severity = severity;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

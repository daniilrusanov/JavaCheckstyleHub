package com.checkstylehub.analyzer.dto;

import com.checkstylehub.analyzer.entity.AnalyzerType;

/**
 * Data Transfer Object for a single static-analysis finding (Checkstyle or PMD).
 */
public class AnalysisResultDto {
    private Long id;
    private String filePath;
    private int lineNumber;
    private String severity;
    private String message;
    private AnalyzerType analyzerType;
    private String codeSnippet;

    public AnalysisResultDto() {
    }

    public AnalysisResultDto(Long id, String filePath, int lineNumber, String severity, String message) {
        this(id, filePath, lineNumber, severity, message, AnalyzerType.CHECKSTYLE);
    }

    public AnalysisResultDto(Long id, String filePath, int lineNumber, String severity, String message,
            AnalyzerType analyzerType) {
        this.id = id;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.severity = severity;
        this.message = message;
        this.analyzerType = analyzerType;
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

    public AnalyzerType getAnalyzerType() {
        return analyzerType != null ? analyzerType : AnalyzerType.CHECKSTYLE;
    }

    public void setAnalyzerType(AnalyzerType analyzerType) {
        this.analyzerType = analyzerType;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public void setCodeSnippet(String codeSnippet) {
        this.codeSnippet = codeSnippet;
    }
}

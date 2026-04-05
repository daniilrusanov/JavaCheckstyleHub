package com.checkstylehub.analyzer.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.ColumnDefault;

import java.util.Objects;

/**
 * Entity representing a single static-analysis finding (Checkstyle or PMD).
 * Each result is associated with a specific file, line number, and severity level.
 */
@Entity
@Table(name = "analysis_results")
@JsonIgnoreProperties(ignoreUnknown = true, value = {"id", "request"})
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private AnalysisRequest request;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private int lineNumber;

    @Column(nullable = false)
    private String severity;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "analyzer_type", nullable = false, length = 32)
    @ColumnDefault("'CHECKSTYLE'")
    private AnalyzerType analyzerType = AnalyzerType.CHECKSTYLE;

    /** ~10 lines of source code surrounding the violation, captured at analysis time. */
    @Column(columnDefinition = "TEXT")
    private String codeSnippet;

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
        this.analyzerType = analyzerType != null ? analyzerType : AnalyzerType.CHECKSTYLE;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public void setCodeSnippet(String codeSnippet) {
        this.codeSnippet = codeSnippet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AnalysisResult that = (AnalysisResult) o;
        return lineNumber == that.lineNumber
                && Objects.equals(id, that.id)
                && Objects.equals(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, filePath, lineNumber);
    }
}

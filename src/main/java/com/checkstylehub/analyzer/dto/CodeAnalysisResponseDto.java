package com.checkstylehub.analyzer.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for direct code analysis response.
 * Contains Checkstyle violations and optional compilation results.
 */
public class CodeAnalysisResponseDto {

    /** Whether the analysis was successful. */
    private boolean success;

    /** Error message if analysis failed. */
    private String errorMessage;

    /** Whether the code compiles successfully. */
    private Boolean compilationSuccess;

    /** Compilation errors if any. */
    private List<CompilationError> compilationErrors = new ArrayList<>();

    /** Checkstyle violations. */
    private List<AnalysisResultDto> violations = new ArrayList<>();

    /** Total number of violations. */
    private int violationCount;

    /** Code quality score (0-100). */
    private Integer qualityScore;

    public CodeAnalysisResponseDto() { }

    public static CodeAnalysisResponseDto error(String message) {
        CodeAnalysisResponseDto response = new CodeAnalysisResponseDto();
        response.setSuccess(false);
        response.setErrorMessage(message);
        return response;
    }

    public static CodeAnalysisResponseDto success(List<AnalysisResultDto> violations) {
        CodeAnalysisResponseDto response = new CodeAnalysisResponseDto();
        response.setSuccess(true);
        response.setViolations(violations);
        response.setViolationCount(violations.size());
        return response;
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Boolean getCompilationSuccess() {
        return compilationSuccess;
    }

    public void setCompilationSuccess(Boolean compilationSuccess) {
        this.compilationSuccess = compilationSuccess;
    }

    public List<CompilationError> getCompilationErrors() {
        return compilationErrors;
    }

    public void setCompilationErrors(List<CompilationError> compilationErrors) {
        this.compilationErrors = compilationErrors;
    }

    public List<AnalysisResultDto> getViolations() {
        return violations;
    }

    public void setViolations(List<AnalysisResultDto> violations) {
        this.violations = violations;
        this.violationCount = violations != null ? violations.size() : 0;
    }

    public int getViolationCount() {
        return violationCount;
    }

    public void setViolationCount(int violationCount) {
        this.violationCount = violationCount;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    /**
     * Represents a compilation error.
     */
    public static class CompilationError {
        private long lineNumber;
        private long columnNumber;
        private String message;
        private String kind; // ERROR, WARNING, NOTE, etc.

        public CompilationError() { }

        public CompilationError(long lineNumber, long columnNumber, String message, String kind) {
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.message = message;
            this.kind = kind;
        }

        public long getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(long lineNumber) {
            this.lineNumber = lineNumber;
        }

        public long getColumnNumber() {
            return columnNumber;
        }

        public void setColumnNumber(long columnNumber) {
            this.columnNumber = columnNumber;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }
    }
}

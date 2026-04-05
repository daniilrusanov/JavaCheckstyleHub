package com.checkstylehub.analyzer.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for direct code analysis response.
 * Contains Checkstyle violations and optional compilation results.
 */
@Data
@NoArgsConstructor
public class CodeAnalysisResponseDto {

    private boolean success;
    private String errorMessage;
    private Boolean compilationSuccess;
    private List<CompilationError> compilationErrors = new ArrayList<>();
    private List<AnalysisResultDto> violations = new ArrayList<>();
    private int violationCount;
    private Integer qualityScore;

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

    /** Custom setter keeps {@code violationCount} in sync with the list size. */
    public void setViolations(List<AnalysisResultDto> violations) {
        this.violations = violations;
        this.violationCount = violations != null ? violations.size() : 0;
    }

    /**
     * Represents a compilation error.
     */
    @Data
    @NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CompilationError {
        private long lineNumber;
        private long columnNumber;
        private String message;
        private String kind;
    }
}

package com.checkstylehub.analyzer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    /** JSON field name remains {@code success} (avoids clash with static factory methods). */
    @JsonProperty("success")
    private boolean successful;
    private String errorMessage;
    private Boolean compilationSuccess;
    private List<CompilationError> compilationErrors = new ArrayList<>();
    private List<AnalysisResultDto> violations = new ArrayList<>();
    private int violationCount;
    private Integer qualityScore;

    public static CodeAnalysisResponseDto error(String message) {
        CodeAnalysisResponseDto response = new CodeAnalysisResponseDto();
        response.setSuccessful(false);
        response.setErrorMessage(message);
        return response;
    }

    public static CodeAnalysisResponseDto withViolations(List<AnalysisResultDto> violations) {
        CodeAnalysisResponseDto response = new CodeAnalysisResponseDto();
        response.setSuccessful(true);
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

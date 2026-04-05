package com.checkstylehub.analyzer.dto;

import com.checkstylehub.analyzer.entity.AnalyzerType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.AccessLevel;

/**
 * Data Transfer Object for a single static-analysis finding (Checkstyle or PMD).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultDto {

    private Long id;
    private String filePath;
    private int lineNumber;
    private String severity;
    private String message;

    @Getter(AccessLevel.NONE)
    private AnalyzerType analyzerType;

    private String codeSnippet;

    /** Null-safe getter — defaults to CHECKSTYLE for legacy rows that have no type set. */
    public AnalyzerType getAnalyzerType() {
        return analyzerType != null ? analyzerType : AnalyzerType.CHECKSTYLE;
    }
}

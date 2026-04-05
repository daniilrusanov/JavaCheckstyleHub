package com.checkstylehub.analyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for stateless AI explanations (e.g. direct code analysis without persisted results).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatelessAiRequestDto {

    /** Full Java source to extract a window around {@link #lineNumber}. */
    private String code;

    /** Checkstyle / violation message text. */
    private String message;

    /** 1-based line number in {@link #code} to center the snippet window on. */
    private int lineNumber;
}

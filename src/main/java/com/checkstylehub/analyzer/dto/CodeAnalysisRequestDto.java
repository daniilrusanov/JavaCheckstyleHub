package com.checkstylehub.analyzer.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for direct code analysis request.
 * Allows users to submit Java code directly without a GitHub repository.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeAnalysisRequestDto {

    /** The Java source code to analyze. */
    private String code;

    /** Optional filename (defaults to "Main.java" if not provided). */
    private String fileName;

    /** Optional custom Checkstyle XML configuration. */
    private String checkstyleConfig;

    /** Whether to check if the code compiles. */
    private boolean checkCompilation = true;
}

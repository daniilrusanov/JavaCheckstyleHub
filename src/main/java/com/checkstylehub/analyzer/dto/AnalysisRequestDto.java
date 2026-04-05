package com.checkstylehub.analyzer.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object for analysis request.
 * Used to receive repository URL and optional Checkstyle configuration from client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequestDto {

    private String repoUrl;
    private String checkstyleConfig;
}

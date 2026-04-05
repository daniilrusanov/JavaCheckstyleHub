package com.checkstylehub.analyzer.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for analysis request status information.
 * Used to provide status updates to the client (PENDING/CLONING/ANALYZING/COMPLETED/FAILED).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequestStatusDto {

    private Long id;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private String repoUrl;
    private Long violationsCount;
    private Integer qualityScore;
}

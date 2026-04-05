package com.checkstylehub.analyzer.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Payload sent to {@code analysis_queue} to run
 * {@link com.checkstylehub.analyzer.service.AnalysisService#startAnalysisFlow}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisQueueMessage {

    private Long requestId;
    private String customCheckstyleConfig;

    /**
     * Alias used by the listener when delegating to {@code startAnalysisFlow(requestId, customConfig)}.
     */
    public String getCheckstyleConfig() {
        return customCheckstyleConfig;
    }
}

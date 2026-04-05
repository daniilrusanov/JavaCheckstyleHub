package com.checkstylehub.analyzer.dto;

/**
 * Payload sent to {@code analysis_queue} to run {@link com.checkstylehub.analyzer.service.AnalysisService#startAnalysisFlow}.
 */
public class AnalysisQueueMessage {

    private Long requestId;
    private String customCheckstyleConfig;

    public AnalysisQueueMessage() {
    }

    public AnalysisQueueMessage(Long requestId, String customCheckstyleConfig) {
        this.requestId = requestId;
        this.customCheckstyleConfig = customCheckstyleConfig;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getCustomCheckstyleConfig() {
        return customCheckstyleConfig;
    }

    public void setCustomCheckstyleConfig(String customCheckstyleConfig) {
        this.customCheckstyleConfig = customCheckstyleConfig;
    }

    /**
     * Alias used by the listener when delegating to {@code startAnalysisFlow(requestId, customConfig)}.
     */
    public String getCheckstyleConfig() {
        return customCheckstyleConfig;
    }
}

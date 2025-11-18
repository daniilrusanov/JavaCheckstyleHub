package com.checkstylehub.analyzer.dto;

/**
 * Data Transfer Object for analysis request.
 * Used to receive repository URL and optional Checkstyle configuration from client.
 */
public class AnalysisRequestDto {

    private String repoUrl;
    private String checkstyleConfig;

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getCheckstyleConfig() {
        return checkstyleConfig;
    }

    public void setCheckstyleConfig(String checkstyleConfig) {
        this.checkstyleConfig = checkstyleConfig;
    }
}

package com.checkstylehub.analyzer.dto;

/**
 * Data Transfer Object for updating Checkstyle configuration.
 * Contains only the XML content to be updated.
 */
public class UpdateCheckstyleConfigurationDto {

    private String xmlContent;

    public UpdateCheckstyleConfigurationDto() {
    }

    public UpdateCheckstyleConfigurationDto(String xmlContent) {
        this.xmlContent = xmlContent;
    }

    // Getters and Setters
    public String getXmlContent() {
        return xmlContent;
    }

    public void setXmlContent(String xmlContent) {
        this.xmlContent = xmlContent;
    }
}


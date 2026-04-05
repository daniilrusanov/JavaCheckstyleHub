package com.checkstylehub.analyzer.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object for updating Checkstyle configuration.
 * Contains only the XML content to be updated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCheckstyleConfigurationDto {

    private String xmlContent;
}

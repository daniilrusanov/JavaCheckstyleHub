package com.checkstylehub.analyzer.dto;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object for Checkstyle XML configuration.
 * Contains the raw XML content and metadata about the configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckstyleConfigurationDto {

    private Long id;
    private String configName;
    private String xmlContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}

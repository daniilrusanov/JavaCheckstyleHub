package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.CheckstyleConfigurationDto;
import com.checkstylehub.analyzer.dto.CheckstyleRulesDto;
import com.checkstylehub.analyzer.dto.UpdateCheckstyleConfigurationDto;
import com.checkstylehub.analyzer.entity.CheckstyleConfiguration;
import com.checkstylehub.analyzer.repository.CheckstyleConfigurationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Service for managing Checkstyle configurations.
 * Handles loading, updating, and converting between XML and structured rule formats.
 */
@Service
public class CheckstyleConfigurationService {

    private static final String DEFAULT_CONFIG_FILE = "default_checkstyle_rules.xml";
    private static final String DEFAULT_CONFIG_NAME = "default";

    private final CheckstyleConfigurationRepository configurationRepository;
    private final CheckstyleXmlConverter xmlConverter;

    public CheckstyleConfigurationService(CheckstyleConfigurationRepository configurationRepository,
                                          CheckstyleXmlConverter xmlConverter) {
        this.configurationRepository = configurationRepository;
        this.xmlConverter = xmlConverter;
    }

    @PostConstruct
    public void ensureDefaultConfigurationExists() {
        try {
            if (configurationRepository.findByIsActiveTrue().isEmpty()) {
                initializeDefaultConfiguration();
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not initialize default configuration on startup: " + e.getMessage());
        }
    }

    /**
     * @deprecated Use getActiveConfigurationAsRules() instead
     */
    @Deprecated
    @Transactional
    public CheckstyleConfigurationDto getActiveConfiguration() {
        CheckstyleConfiguration config = configurationRepository.findByIsActiveTrue()
                .orElseGet(this::initializeDefaultConfiguration);

        return toDto(config);
    }

    @Transactional
    public CheckstyleRulesDto getActiveConfigurationAsRules() {
        CheckstyleConfiguration config = configurationRepository.findByIsActiveTrue()
                .orElseGet(this::initializeDefaultConfiguration);

        CheckstyleRulesDto rulesDto = xmlConverter.parseXmlToDto(config.getXmlContent());
        rulesDto.setId(config.getId());
        rulesDto.setConfigName(config.getConfigName());
        rulesDto.setCreatedAt(config.getCreatedAt());
        rulesDto.setUpdatedAt(config.getUpdatedAt());
        rulesDto.setIsActive(config.getIsActive());

        return rulesDto;
    }

    /**
     * @deprecated Use updateActiveConfigurationFromRules() instead
     */
    @Deprecated
    @Transactional
    public CheckstyleConfigurationDto updateActiveConfiguration(UpdateCheckstyleConfigurationDto updateDto) {
        CheckstyleConfiguration config = configurationRepository.findByIsActiveTrue()
                .orElseGet(this::initializeDefaultConfiguration);

        if (updateDto.getXmlContent() != null && !updateDto.getXmlContent().isBlank()) {
            config.setXmlContent(updateDto.getXmlContent());
        }

        CheckstyleConfiguration savedConfig = configurationRepository.save(config);
        return toDto(savedConfig);
    }

    @Transactional
    public CheckstyleRulesDto updateActiveConfigurationFromRules(CheckstyleRulesDto rulesDto) {
        CheckstyleConfiguration config = configurationRepository.findByIsActiveTrue()
                .orElseGet(this::initializeDefaultConfiguration);

        String newXml = xmlConverter.generateXmlFromDto(rulesDto);
        config.setXmlContent(newXml);

        CheckstyleConfiguration savedConfig = configurationRepository.save(config);

        CheckstyleRulesDto result = xmlConverter.parseXmlToDto(savedConfig.getXmlContent());
        result.setId(savedConfig.getId());
        result.setConfigName(savedConfig.getConfigName());
        result.setCreatedAt(savedConfig.getCreatedAt());
        result.setUpdatedAt(savedConfig.getUpdatedAt());
        result.setIsActive(savedConfig.getIsActive());

        return result;
    }

    @Transactional(readOnly = true)
    public String getActiveConfigurationXml() {
        return configurationRepository.findByIsActiveTrue()
                .map(CheckstyleConfiguration::getXmlContent)
                .orElseGet(this::loadDefaultConfigurationXml);
    }

    @Transactional
    protected CheckstyleConfiguration initializeDefaultConfiguration() {
        String defaultXml = loadDefaultConfigurationXml();

        CheckstyleConfiguration config = new CheckstyleConfiguration(
                DEFAULT_CONFIG_NAME,
                defaultXml,
                true
        );

        return configurationRepository.save(config);
    }

    private String loadDefaultConfigurationXml() {
        try {
            ClassPathResource resource = new ClassPathResource(DEFAULT_CONFIG_FILE);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load default Checkstyle configuration", e);
        }
    }

    private CheckstyleConfigurationDto toDto(CheckstyleConfiguration entity) {
        return new CheckstyleConfigurationDto(
                entity.getId(),
                entity.getConfigName(),
                entity.getXmlContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getIsActive()
        );
    }
}


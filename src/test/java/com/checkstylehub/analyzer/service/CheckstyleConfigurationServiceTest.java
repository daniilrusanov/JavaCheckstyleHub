package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.CheckstyleConfigurationDto;
import com.checkstylehub.analyzer.dto.CheckstyleRulesDto;
import com.checkstylehub.analyzer.dto.UpdateCheckstyleConfigurationDto;
import com.checkstylehub.analyzer.entity.CheckstyleConfiguration;
import com.checkstylehub.analyzer.repository.CheckstyleConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CheckstyleConfigurationService.
 * Tests configuration management and XML/DTO conversion.
 */
class CheckstyleConfigurationServiceTest {

    @Mock
    private CheckstyleConfigurationRepository configurationRepository;

    @Mock
    private CheckstyleXmlConverter xmlConverter;

    @InjectMocks
    private CheckstyleConfigurationService configurationService;

    private CheckstyleConfiguration mockConfig;
    private String sampleXml;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sampleXml = """
                <?xml version="1.0"?>
                <!DOCTYPE module PUBLIC
                    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
                    "https://checkstyle.org/dtds/configuration_1_3.dtd">
                <module name="Checker">
                    <property name="charset" value="UTF-8"/>
                    <module name="TreeWalker">
                        <module name="EmptyStatement"/>
                    </module>
                </module>
                """;

        mockConfig = new CheckstyleConfiguration("default", sampleXml, true);
        mockConfig.setId(1L);
        mockConfig.setCreatedAt(LocalDateTime.now());
        mockConfig.setUpdatedAt(LocalDateTime.now());

        System.out.println("Початок тесту CheckstyleConfigurationService");
    }

    @Test
    @DisplayName("Should get active configuration as DTO")
    void testGetActiveConfiguration() {
        System.out.println("Тест: отримання активної конфігурації");

        when(configurationRepository.findByIsActiveTrue()).thenReturn(Optional.of(mockConfig));

        CheckstyleConfigurationDto result = configurationService.getActiveConfiguration();

        assertNotNull(result, "Конфігурація не повинна бути null");
        assertEquals("default", result.getConfigName());
        assertEquals(sampleXml, result.getXmlContent());
        assertTrue(result.getIsActive());

        System.out.println("Активну конфігурацію успішно отримано: " + result.getConfigName());
    }

    @Test
    @DisplayName("Should get active configuration as rules DTO")
    void testGetActiveConfigurationAsRules() {
        System.out.println("Тест: отримання конфігурації як структурованих правил");

        CheckstyleRulesDto rulesDto = new CheckstyleRulesDto();
        rulesDto.setCharset("UTF-8");
        rulesDto.setLineLength(120);
        rulesDto.setEmptyStatement(true);

        when(configurationRepository.findByIsActiveTrue()).thenReturn(Optional.of(mockConfig));
        when(xmlConverter.parseXmlToDto(sampleXml)).thenReturn(rulesDto);

        CheckstyleRulesDto result = configurationService.getActiveConfigurationAsRules();

        assertNotNull(result, "Правила не повинні бути null");
        assertEquals("UTF-8", result.getCharset());
        assertEquals(120, result.getLineLength());
        assertTrue(result.getEmptyStatement());

        System.out.println("Конфігурацію успішно конвертовано у структуровані правила");
    }

    @Test
    @DisplayName("Should initialize default configuration when none exists")
    void testGetActiveConfiguration_InitializesDefault() {
        System.out.println("Тест: ініціалізація дефолтної конфігурації");

        when(configurationRepository.findByIsActiveTrue()).thenReturn(Optional.empty());
        when(configurationRepository.save(any(CheckstyleConfiguration.class))).thenReturn(mockConfig);

        CheckstyleConfigurationDto result = configurationService.getActiveConfiguration();

        assertNotNull(result, "Конфігурація не повинна бути null");
        verify(configurationRepository, times(1)).save(any(CheckstyleConfiguration.class));

        System.out.println("Дефолтну конфігурацію успішно ініціалізовано");
    }

    @Test
    @DisplayName("Should update active configuration from XML")
    void testUpdateActiveConfiguration() {
        System.out.println("Тест: оновлення конфігурації з XML");

        String newXml = "<module name=\"Checker\"><module name=\"TreeWalker\"></module></module>";
        UpdateCheckstyleConfigurationDto updateDto = new UpdateCheckstyleConfigurationDto(newXml);

        CheckstyleConfiguration updatedConfig = new CheckstyleConfiguration("default", newXml, true);
        updatedConfig.setId(1L);

        when(configurationRepository.findByIsActiveTrue()).thenReturn(Optional.of(mockConfig));
        when(configurationRepository.save(any(CheckstyleConfiguration.class))).thenReturn(updatedConfig);

        CheckstyleConfigurationDto result = configurationService.updateActiveConfiguration(updateDto);

        assertNotNull(result, "Оновлена конфігурація не повинна бути null");
        assertEquals(newXml, result.getXmlContent());
        verify(configurationRepository, times(1)).save(any(CheckstyleConfiguration.class));

        System.out.println("Конфігурацію успішно оновлено з XML");
    }

    @Test
    @DisplayName("Should update active configuration from rules DTO")
    void testUpdateActiveConfigurationFromRules() {
        System.out.println("Тест: оновлення конфігурації зі структурованих правил");

        CheckstyleRulesDto rulesDto = new CheckstyleRulesDto();
        rulesDto.setCharset("UTF-8");
        rulesDto.setLineLength(100);
        rulesDto.setEmptyStatement(false);

        String generatedXml = "<module name=\"Checker\"></module>";
        CheckstyleConfiguration updatedConfig = new CheckstyleConfiguration("default", generatedXml, true);
        updatedConfig.setId(1L);

        when(configurationRepository.findByIsActiveTrue()).thenReturn(Optional.of(mockConfig));
        when(xmlConverter.generateXmlFromDto(rulesDto)).thenReturn(generatedXml);
        when(configurationRepository.save(any(CheckstyleConfiguration.class))).thenReturn(updatedConfig);
        when(xmlConverter.parseXmlToDto(generatedXml)).thenReturn(rulesDto);

        CheckstyleRulesDto result = configurationService.updateActiveConfigurationFromRules(rulesDto);

        assertNotNull(result, "Оновлені правила не повинні бути null");
        verify(xmlConverter, times(1)).generateXmlFromDto(rulesDto);
        verify(configurationRepository, times(1)).save(any(CheckstyleConfiguration.class));

        System.out.println("Конфігурацію успішно оновлено зі структурованих правил");
    }

    @Test
    @DisplayName("Should get active configuration XML content")
    void testGetActiveConfigurationXml() {
        System.out.println("Тест: отримання XML контенту активної конфігурації");

        when(configurationRepository.findByIsActiveTrue()).thenReturn(Optional.of(mockConfig));

        String result = configurationService.getActiveConfigurationXml();

        assertNotNull(result, "XML контент не повинен бути null");
        assertEquals(sampleXml, result);

        System.out.println("XML контент успішно отримано");
    }

    @Test
    @DisplayName("Should load default XML when no active configuration exists")
    void testGetActiveConfigurationXml_LoadsDefault() {
        System.out.println("Тест: завантаження дефолтного XML");

        when(configurationRepository.findByIsActiveTrue()).thenReturn(Optional.empty());

        String result = configurationService.getActiveConfigurationXml();

        assertNotNull(result, "Дефолтний XML не повинен бути null");
        assertFalse(result.isEmpty(), "Дефолтний XML не повинен бути порожнім");

        System.out.println("Дефолтний XML успішно завантажено");
    }

    @Test
    @DisplayName("Should not update configuration when XML content is blank")
    void testUpdateActiveConfiguration_BlankXml() {
        System.out.println("Тест: оновлення конфігурації з порожнім XML");

        UpdateCheckstyleConfigurationDto updateDto = new UpdateCheckstyleConfigurationDto("");

        when(configurationRepository.findByIsActiveTrue()).thenReturn(Optional.of(mockConfig));
        when(configurationRepository.save(any(CheckstyleConfiguration.class))).thenReturn(mockConfig);

        CheckstyleConfigurationDto result = configurationService.updateActiveConfiguration(updateDto);

        assertNotNull(result, "Результат не повинен бути null");
        assertEquals(sampleXml, result.getXmlContent(), "XML не повинен змінитись");

        System.out.println("Конфігурація не змінилась при порожньому XML (очікувано)");
    }
}


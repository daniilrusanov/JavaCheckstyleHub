package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.CheckstyleRulesDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CheckstyleXmlConverter.
 * Tests XML to DTO and DTO to XML conversion.
 */
class CheckstyleXmlConverterTest {

    private CheckstyleXmlConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CheckstyleXmlConverter();
        System.out.println("Початок тесту CheckstyleXmlConverter");
    }

    @Test
    @DisplayName("Should parse XML to DTO correctly")
    void testParseXmlToDto() {
        System.out.println("Тест: парсинг XML у DTO");

        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE module PUBLIC
                    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
                    "https://checkstyle.org/dtds/configuration_1_3.dtd">
                <module name="Checker">
                    <property name="charset" value="UTF-8"/>
                    <property name="severity" value="warning"/>
                    <property name="fileExtensions" value="java, properties, xml"/>
                
                    <module name="LineLength">
                        <property name="max" value="120"/>
                        <property name="ignorePattern" value="^package.*|^import.*"/>
                    </module>
                
                    <module name="TreeWalker">
                        <module name="EmptyStatement"/>
                        <module name="NeedBraces"/>
                        <module name="LeftCurly"/>
                    </module>
                </module>
                """;

        CheckstyleRulesDto dto = converter.parseXmlToDto(xml);

        assertNotNull(dto, "DTO не повинно бути null");
        assertEquals("UTF-8", dto.getCharset());
        assertEquals("warning", dto.getSeverity());
        assertEquals("java, properties, xml", dto.getFileExtensions());
        assertEquals(120, dto.getLineLength());
        assertTrue(dto.getEmptyStatement());
        assertTrue(dto.getNeedBraces());
        assertTrue(dto.getLeftCurly());

        System.out.println("XML успішно розпарсено у DTO");
    }

    @Test
    @DisplayName("Should generate XML from DTO correctly")
    void testGenerateXmlFromDto() {
        System.out.println("Тест: генерація XML з DTO");

        CheckstyleRulesDto dto = new CheckstyleRulesDto();
        dto.setCharset("UTF-8");
        dto.setSeverity("error");
        dto.setFileExtensions("java");
        dto.setLineLength(100);
        dto.setLineLengthIgnorePattern("^import.*");
        dto.setEmptyStatement(true);
        dto.setNeedBraces(false);
        dto.setLeftCurly(true);

        String xml = converter.generateXmlFromDto(dto);

        assertNotNull(xml, "XML не повинен бути null");
        assertFalse(xml.isEmpty(), "XML не повинен бути порожнім");
        assertTrue(xml.contains("Checker"), "XML має містити Checker");
        assertTrue(xml.contains("charset"), "XML має містити charset");
        assertTrue(xml.contains("UTF-8"), "XML має містити значення UTF-8");
        assertTrue(xml.contains("EmptyStatement"), "XML має містити EmptyStatement");
        assertTrue(xml.contains("LeftCurly"), "XML має містити LeftCurly");
        assertFalse(xml.contains("NeedBraces"), "XML не повинен містити NeedBraces (вимкнено)");

        System.out.println("XML успішно згенеровано з DTO");
    }

    @Test
    @DisplayName("Should convert DTO to XML and back to DTO preserving data")
    void testRoundTripConversion() {
        System.out.println("Тест: двостороння конвертація DTO ↔ XML");

        CheckstyleRulesDto originalDto = new CheckstyleRulesDto();
        originalDto.setCharset("UTF-8");
        originalDto.setSeverity("warning");
        originalDto.setFileExtensions("java, xml");
        originalDto.setLineLength(120);
        originalDto.setEmptyStatement(true);
        originalDto.setNeedBraces(true);
        originalDto.setLeftCurly(false);
        originalDto.setRightCurly(true);
        originalDto.setAvoidStarImport(true);

        String xml = converter.generateXmlFromDto(originalDto);
        CheckstyleRulesDto parsedDto = converter.parseXmlToDto(xml);

        assertEquals(originalDto.getCharset(), parsedDto.getCharset());
        assertEquals(originalDto.getSeverity(), parsedDto.getSeverity());
        assertEquals(originalDto.getFileExtensions(), parsedDto.getFileExtensions());
        assertEquals(originalDto.getLineLength(), parsedDto.getLineLength());
        assertEquals(originalDto.getEmptyStatement(), parsedDto.getEmptyStatement());
        assertEquals(originalDto.getNeedBraces(), parsedDto.getNeedBraces());
        assertEquals(originalDto.getLeftCurly(), parsedDto.getLeftCurly());
        assertEquals(originalDto.getRightCurly(), parsedDto.getRightCurly());
        assertEquals(originalDto.getAvoidStarImport(), parsedDto.getAvoidStarImport());

        System.out.println("Двостороння конвертація пройшла успішно, дані збережено");
    }

    @Test
    @DisplayName("Should handle DTO with all rules disabled")
    void testGenerateXml_AllRulesDisabled() {
        System.out.println("Тест: генерація XML з усіма вимкненими правилами");

        CheckstyleRulesDto dto = new CheckstyleRulesDto();
        dto.setCharset("UTF-8");
        dto.setSeverity("info");
        dto.setFileExtensions("java");
        dto.setEmptyStatement(false);
        dto.setNeedBraces(false);
        dto.setLeftCurly(false);
        dto.setRightCurly(false);
        dto.setAvoidStarImport(false);

        String xml = converter.generateXmlFromDto(dto);

        assertNotNull(xml, "XML не повинен бути null");
        assertTrue(xml.contains("Checker"), "XML має містити Checker");
        assertTrue(xml.contains("TreeWalker"), "XML має містити TreeWalker");

        System.out.println("XML з вимкненими правилами згенеровано успішно");
    }

    @Test
    @DisplayName("Should throw exception for invalid XML")
    void testParseXmlToDto_InvalidXml() {
        System.out.println("Тест: парсинг невалідного XML");

        String invalidXml = "<invalid>xml</not-closed>";

        assertThrows(RuntimeException.class, () -> {
            converter.parseXmlToDto(invalidXml);
        }, "Має викинути RuntimeException для невалідного XML");

        System.out.println("Виняток коректно викинуто для невалідного XML");
    }

    @Test
    @DisplayName("Should parse XML with minimal configuration")
    void testParseXmlToDto_MinimalXml() {
        System.out.println("Тест: парсинг мінімального XML");

        String minimalXml = """
                <?xml version="1.0"?>
                <!DOCTYPE module PUBLIC
                    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
                    "https://checkstyle.org/dtds/configuration_1_3.dtd">
                <module name="Checker">
                    <module name="TreeWalker">
                    </module>
                </module>
                """;

        CheckstyleRulesDto dto = converter.parseXmlToDto(minimalXml);

        assertNotNull(dto, "DTO не повинно бути null");
        assertNotNull(dto.getCharset(), "Charset має мати значення за замовчуванням");

        System.out.println("Мінімальний XML успішно розпарсено");
    }

    @Test
    @DisplayName("Should handle LineLength with custom ignorePattern")
    void testGenerateXml_CustomLineLengthPattern() {
        System.out.println("Тест: генерація XML з кастомним patternом для LineLength");

        CheckstyleRulesDto dto = new CheckstyleRulesDto();
        dto.setCharset("UTF-8");
        dto.setSeverity("warning");
        dto.setFileExtensions("java");
        dto.setLineLength(150);
        dto.setLineLengthIgnorePattern("^package.*|^import.*|http://");

        String xml = converter.generateXmlFromDto(dto);

        assertNotNull(xml, "XML не повинен бути null");
        assertTrue(xml.contains("LineLength"), "XML має містити LineLength");
        assertTrue(xml.contains("150"), "XML має містити значення 150");
        assertTrue(xml.contains("ignorePattern"), "XML має містити ignorePattern");

        System.out.println("XML з кастомним LineLength згенеровано успішно");
    }
}


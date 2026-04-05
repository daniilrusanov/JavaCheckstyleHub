package com.checkstylehub.analyzer.dto;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for structured Checkstyle rules configuration.
 * Instead of raw XML, each rule is represented as a separate field.
 * Used for easier frontend configuration editing.
 */
@Data
@NoArgsConstructor
public class CheckstyleRulesDto {

    private Long id;
    private String configName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;

    private String charset = "UTF-8";
    private String severity = "warning";
    private String fileExtensions = "java, properties, xml";

    private Integer lineLength = 120;
    private String lineLengthIgnorePattern = "^package.*|^import.*|a href|href|http://|https://|ftp://";

    private Boolean avoidStarImport = true;
    private Boolean oneTopLevelClass = true;
    private Boolean noLineWrap = true;

    private Boolean emptyBlock = true;
    private Boolean needBraces = true;
    private Boolean leftCurly = true;
    private Boolean rightCurly = true;

    private Boolean emptyStatement = true;
    private Boolean equalsHashCode = true;
    private Boolean illegalInstantiation = true;
    private Boolean missingSwitchDefault = true;
    private Boolean simplifyBooleanExpression = true;
    private Boolean simplifyBooleanReturn = true;

    private Boolean finalClass = true;
    private Boolean hideUtilityClassConstructor = true;
    private Boolean interfaceIsType = true;
    private Boolean visibilityModifier = true;

    private Boolean outerTypeFilename = true;
    private Boolean illegalTokenText = true;
    private Boolean avoidEscapedUnicodeCharacters = true;
}

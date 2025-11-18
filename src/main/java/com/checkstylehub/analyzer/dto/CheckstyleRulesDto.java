package com.checkstylehub.analyzer.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for structured Checkstyle rules configuration.
 * Instead of raw XML, each rule is represented as a separate field.
 * Used for easier frontend configuration editing.
 */
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

    public CheckstyleRulesDto() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(String fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public Integer getLineLength() {
        return lineLength;
    }

    public void setLineLength(Integer lineLength) {
        this.lineLength = lineLength;
    }

    public String getLineLengthIgnorePattern() {
        return lineLengthIgnorePattern;
    }

    public void setLineLengthIgnorePattern(String lineLengthIgnorePattern) {
        this.lineLengthIgnorePattern = lineLengthIgnorePattern;
    }

    public Boolean getAvoidStarImport() {
        return avoidStarImport;
    }

    public void setAvoidStarImport(Boolean avoidStarImport) {
        this.avoidStarImport = avoidStarImport;
    }

    public Boolean getOneTopLevelClass() {
        return oneTopLevelClass;
    }

    public void setOneTopLevelClass(Boolean oneTopLevelClass) {
        this.oneTopLevelClass = oneTopLevelClass;
    }

    public Boolean getNoLineWrap() {
        return noLineWrap;
    }

    public void setNoLineWrap(Boolean noLineWrap) {
        this.noLineWrap = noLineWrap;
    }

    public Boolean getEmptyBlock() {
        return emptyBlock;
    }

    public void setEmptyBlock(Boolean emptyBlock) {
        this.emptyBlock = emptyBlock;
    }

    public Boolean getNeedBraces() {
        return needBraces;
    }

    public void setNeedBraces(Boolean needBraces) {
        this.needBraces = needBraces;
    }

    public Boolean getLeftCurly() {
        return leftCurly;
    }

    public void setLeftCurly(Boolean leftCurly) {
        this.leftCurly = leftCurly;
    }

    public Boolean getRightCurly() {
        return rightCurly;
    }

    public void setRightCurly(Boolean rightCurly) {
        this.rightCurly = rightCurly;
    }

    public Boolean getEmptyStatement() {
        return emptyStatement;
    }

    public void setEmptyStatement(Boolean emptyStatement) {
        this.emptyStatement = emptyStatement;
    }

    public Boolean getEqualsHashCode() {
        return equalsHashCode;
    }

    public void setEqualsHashCode(Boolean equalsHashCode) {
        this.equalsHashCode = equalsHashCode;
    }

    public Boolean getIllegalInstantiation() {
        return illegalInstantiation;
    }

    public void setIllegalInstantiation(Boolean illegalInstantiation) {
        this.illegalInstantiation = illegalInstantiation;
    }

    public Boolean getMissingSwitchDefault() {
        return missingSwitchDefault;
    }

    public void setMissingSwitchDefault(Boolean missingSwitchDefault) {
        this.missingSwitchDefault = missingSwitchDefault;
    }

    public Boolean getSimplifyBooleanExpression() {
        return simplifyBooleanExpression;
    }

    public void setSimplifyBooleanExpression(Boolean simplifyBooleanExpression) {
        this.simplifyBooleanExpression = simplifyBooleanExpression;
    }

    public Boolean getSimplifyBooleanReturn() {
        return simplifyBooleanReturn;
    }

    public void setSimplifyBooleanReturn(Boolean simplifyBooleanReturn) {
        this.simplifyBooleanReturn = simplifyBooleanReturn;
    }

    public Boolean getFinalClass() {
        return finalClass;
    }

    public void setFinalClass(Boolean finalClass) {
        this.finalClass = finalClass;
    }

    public Boolean getHideUtilityClassConstructor() {
        return hideUtilityClassConstructor;
    }

    public void setHideUtilityClassConstructor(Boolean hideUtilityClassConstructor) {
        this.hideUtilityClassConstructor = hideUtilityClassConstructor;
    }

    public Boolean getInterfaceIsType() {
        return interfaceIsType;
    }

    public void setInterfaceIsType(Boolean interfaceIsType) {
        this.interfaceIsType = interfaceIsType;
    }

    public Boolean getVisibilityModifier() {
        return visibilityModifier;
    }

    public void setVisibilityModifier(Boolean visibilityModifier) {
        this.visibilityModifier = visibilityModifier;
    }

    public Boolean getOuterTypeFilename() {
        return outerTypeFilename;
    }

    public void setOuterTypeFilename(Boolean outerTypeFilename) {
        this.outerTypeFilename = outerTypeFilename;
    }

    public Boolean getIllegalTokenText() {
        return illegalTokenText;
    }

    public void setIllegalTokenText(Boolean illegalTokenText) {
        this.illegalTokenText = illegalTokenText;
    }

    public Boolean getAvoidEscapedUnicodeCharacters() {
        return avoidEscapedUnicodeCharacters;
    }

    public void setAvoidEscapedUnicodeCharacters(Boolean avoidEscapedUnicodeCharacters) {
        this.avoidEscapedUnicodeCharacters = avoidEscapedUnicodeCharacters;
    }
}


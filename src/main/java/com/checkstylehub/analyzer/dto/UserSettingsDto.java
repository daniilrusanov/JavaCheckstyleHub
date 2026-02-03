package com.checkstylehub.analyzer.dto;

import com.checkstylehub.analyzer.entity.UserSettings;

/**
 * DTO for user Checkstyle settings.
 */
public class UserSettingsDto {

    private String charset;
    private String severity;
    private String fileExtensions;
    private Integer lineLength;
    private String lineLengthIgnorePattern;

    private Boolean avoidStarImport;
    private Boolean oneTopLevelClass;
    private Boolean noLineWrap;
    private Boolean emptyBlock;
    private Boolean needBraces;
    private Boolean leftCurly;
    private Boolean rightCurly;
    private Boolean emptyStatement;
    private Boolean equalsHashCode;
    private Boolean illegalInstantiation;
    private Boolean missingSwitchDefault;
    private Boolean simplifyBooleanExpression;
    private Boolean simplifyBooleanReturn;
    private Boolean finalClass;
    private Boolean hideUtilityClassConstructor;
    private Boolean interfaceIsType;
    private Boolean visibilityModifier;
    private Boolean outerTypeFilename;
    private Boolean illegalTokenText;
    private Boolean avoidEscapedUnicodeCharacters;

    public UserSettingsDto() {
    }

    public UserSettingsDto(UserSettings settings) {
        this.charset = settings.getCharset();
        this.severity = settings.getSeverity();
        this.fileExtensions = settings.getFileExtensions();
        this.lineLength = settings.getLineLength();
        this.lineLengthIgnorePattern = settings.getLineLengthIgnorePattern();
        this.avoidStarImport = settings.getAvoidStarImport();
        this.oneTopLevelClass = settings.getOneTopLevelClass();
        this.noLineWrap = settings.getNoLineWrap();
        this.emptyBlock = settings.getEmptyBlock();
        this.needBraces = settings.getNeedBraces();
        this.leftCurly = settings.getLeftCurly();
        this.rightCurly = settings.getRightCurly();
        this.emptyStatement = settings.getEmptyStatement();
        this.equalsHashCode = settings.getEqualsHashCode();
        this.illegalInstantiation = settings.getIllegalInstantiation();
        this.missingSwitchDefault = settings.getMissingSwitchDefault();
        this.simplifyBooleanExpression = settings.getSimplifyBooleanExpression();
        this.simplifyBooleanReturn = settings.getSimplifyBooleanReturn();
        this.finalClass = settings.getFinalClass();
        this.hideUtilityClassConstructor = settings.getHideUtilityClassConstructor();
        this.interfaceIsType = settings.getInterfaceIsType();
        this.visibilityModifier = settings.getVisibilityModifier();
        this.outerTypeFilename = settings.getOuterTypeFilename();
        this.illegalTokenText = settings.getIllegalTokenText();
        this.avoidEscapedUnicodeCharacters = settings.getAvoidEscapedUnicodeCharacters();
    }

    /**
     * Creates default settings with all rules enabled.
     */
    public static UserSettingsDto createDefault() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.charset = "UTF-8";
        dto.severity = "warning";
        dto.fileExtensions = "java, properties, xml";
        dto.lineLength = 120;
        dto.lineLengthIgnorePattern = "^package.*|^import.*|a href|href|http://|https://|ftp://";
        dto.avoidStarImport = true;
        dto.oneTopLevelClass = true;
        dto.noLineWrap = true;
        dto.emptyBlock = true;
        dto.needBraces = true;
        dto.leftCurly = true;
        dto.rightCurly = true;
        dto.emptyStatement = true;
        dto.equalsHashCode = true;
        dto.illegalInstantiation = true;
        dto.missingSwitchDefault = true;
        dto.simplifyBooleanExpression = true;
        dto.simplifyBooleanReturn = true;
        dto.finalClass = true;
        dto.hideUtilityClassConstructor = true;
        dto.interfaceIsType = true;
        dto.visibilityModifier = true;
        dto.outerTypeFilename = true;
        dto.illegalTokenText = true;
        dto.avoidEscapedUnicodeCharacters = true;
        return dto;
    }

    /**
     * Apply DTO values to entity.
     */
    public void applyTo(UserSettings settings) {
        if (charset != null) settings.setCharset(charset);
        if (severity != null) settings.setSeverity(severity);
        if (fileExtensions != null) settings.setFileExtensions(fileExtensions);
        if (lineLength != null) settings.setLineLength(lineLength);
        if (lineLengthIgnorePattern != null) settings.setLineLengthIgnorePattern(lineLengthIgnorePattern);
        if (avoidStarImport != null) settings.setAvoidStarImport(avoidStarImport);
        if (oneTopLevelClass != null) settings.setOneTopLevelClass(oneTopLevelClass);
        if (noLineWrap != null) settings.setNoLineWrap(noLineWrap);
        if (emptyBlock != null) settings.setEmptyBlock(emptyBlock);
        if (needBraces != null) settings.setNeedBraces(needBraces);
        if (leftCurly != null) settings.setLeftCurly(leftCurly);
        if (rightCurly != null) settings.setRightCurly(rightCurly);
        if (emptyStatement != null) settings.setEmptyStatement(emptyStatement);
        if (equalsHashCode != null) settings.setEqualsHashCode(equalsHashCode);
        if (illegalInstantiation != null) settings.setIllegalInstantiation(illegalInstantiation);
        if (missingSwitchDefault != null) settings.setMissingSwitchDefault(missingSwitchDefault);
        if (simplifyBooleanExpression != null) settings.setSimplifyBooleanExpression(simplifyBooleanExpression);
        if (simplifyBooleanReturn != null) settings.setSimplifyBooleanReturn(simplifyBooleanReturn);
        if (finalClass != null) settings.setFinalClass(finalClass);
        if (hideUtilityClassConstructor != null) settings.setHideUtilityClassConstructor(hideUtilityClassConstructor);
        if (interfaceIsType != null) settings.setInterfaceIsType(interfaceIsType);
        if (visibilityModifier != null) settings.setVisibilityModifier(visibilityModifier);
        if (outerTypeFilename != null) settings.setOuterTypeFilename(outerTypeFilename);
        if (illegalTokenText != null) settings.setIllegalTokenText(illegalTokenText);
        if (avoidEscapedUnicodeCharacters != null) settings.setAvoidEscapedUnicodeCharacters(avoidEscapedUnicodeCharacters);
    }

    // Getters and Setters

    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getFileExtensions() { return fileExtensions; }
    public void setFileExtensions(String fileExtensions) { this.fileExtensions = fileExtensions; }

    public Integer getLineLength() { return lineLength; }
    public void setLineLength(Integer lineLength) { this.lineLength = lineLength; }

    public String getLineLengthIgnorePattern() { return lineLengthIgnorePattern; }
    public void setLineLengthIgnorePattern(String lineLengthIgnorePattern) { this.lineLengthIgnorePattern = lineLengthIgnorePattern; }

    public Boolean getAvoidStarImport() { return avoidStarImport; }
    public void setAvoidStarImport(Boolean avoidStarImport) { this.avoidStarImport = avoidStarImport; }

    public Boolean getOneTopLevelClass() { return oneTopLevelClass; }
    public void setOneTopLevelClass(Boolean oneTopLevelClass) { this.oneTopLevelClass = oneTopLevelClass; }

    public Boolean getNoLineWrap() { return noLineWrap; }
    public void setNoLineWrap(Boolean noLineWrap) { this.noLineWrap = noLineWrap; }

    public Boolean getEmptyBlock() { return emptyBlock; }
    public void setEmptyBlock(Boolean emptyBlock) { this.emptyBlock = emptyBlock; }

    public Boolean getNeedBraces() { return needBraces; }
    public void setNeedBraces(Boolean needBraces) { this.needBraces = needBraces; }

    public Boolean getLeftCurly() { return leftCurly; }
    public void setLeftCurly(Boolean leftCurly) { this.leftCurly = leftCurly; }

    public Boolean getRightCurly() { return rightCurly; }
    public void setRightCurly(Boolean rightCurly) { this.rightCurly = rightCurly; }

    public Boolean getEmptyStatement() { return emptyStatement; }
    public void setEmptyStatement(Boolean emptyStatement) { this.emptyStatement = emptyStatement; }

    public Boolean getEqualsHashCode() { return equalsHashCode; }
    public void setEqualsHashCode(Boolean equalsHashCode) { this.equalsHashCode = equalsHashCode; }

    public Boolean getIllegalInstantiation() { return illegalInstantiation; }
    public void setIllegalInstantiation(Boolean illegalInstantiation) { this.illegalInstantiation = illegalInstantiation; }

    public Boolean getMissingSwitchDefault() { return missingSwitchDefault; }
    public void setMissingSwitchDefault(Boolean missingSwitchDefault) { this.missingSwitchDefault = missingSwitchDefault; }

    public Boolean getSimplifyBooleanExpression() { return simplifyBooleanExpression; }
    public void setSimplifyBooleanExpression(Boolean simplifyBooleanExpression) { this.simplifyBooleanExpression = simplifyBooleanExpression; }

    public Boolean getSimplifyBooleanReturn() { return simplifyBooleanReturn; }
    public void setSimplifyBooleanReturn(Boolean simplifyBooleanReturn) { this.simplifyBooleanReturn = simplifyBooleanReturn; }

    public Boolean getFinalClass() { return finalClass; }
    public void setFinalClass(Boolean finalClass) { this.finalClass = finalClass; }

    public Boolean getHideUtilityClassConstructor() { return hideUtilityClassConstructor; }
    public void setHideUtilityClassConstructor(Boolean hideUtilityClassConstructor) { this.hideUtilityClassConstructor = hideUtilityClassConstructor; }

    public Boolean getInterfaceIsType() { return interfaceIsType; }
    public void setInterfaceIsType(Boolean interfaceIsType) { this.interfaceIsType = interfaceIsType; }

    public Boolean getVisibilityModifier() { return visibilityModifier; }
    public void setVisibilityModifier(Boolean visibilityModifier) { this.visibilityModifier = visibilityModifier; }

    public Boolean getOuterTypeFilename() { return outerTypeFilename; }
    public void setOuterTypeFilename(Boolean outerTypeFilename) { this.outerTypeFilename = outerTypeFilename; }

    public Boolean getIllegalTokenText() { return illegalTokenText; }
    public void setIllegalTokenText(Boolean illegalTokenText) { this.illegalTokenText = illegalTokenText; }

    public Boolean getAvoidEscapedUnicodeCharacters() { return avoidEscapedUnicodeCharacters; }
    public void setAvoidEscapedUnicodeCharacters(Boolean avoidEscapedUnicodeCharacters) { this.avoidEscapedUnicodeCharacters = avoidEscapedUnicodeCharacters; }
}

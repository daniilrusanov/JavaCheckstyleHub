package com.checkstylehub.analyzer.dto;

import com.checkstylehub.analyzer.entity.UserSettings;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user Checkstyle settings.
 */
@Data
@NoArgsConstructor
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
}

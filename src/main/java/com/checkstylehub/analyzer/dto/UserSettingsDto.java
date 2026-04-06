package com.checkstylehub.analyzer.dto;

import com.checkstylehub.analyzer.entity.UserSettings;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Consumer;

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
        copyIfNotNull(charset, settings::setCharset);
        copyIfNotNull(severity, settings::setSeverity);
        copyIfNotNull(fileExtensions, settings::setFileExtensions);
        copyIfNotNull(lineLength, settings::setLineLength);
        copyIfNotNull(lineLengthIgnorePattern, settings::setLineLengthIgnorePattern);
        copyIfNotNull(avoidStarImport, settings::setAvoidStarImport);
        copyIfNotNull(oneTopLevelClass, settings::setOneTopLevelClass);
        copyIfNotNull(noLineWrap, settings::setNoLineWrap);
        copyIfNotNull(emptyBlock, settings::setEmptyBlock);
        copyIfNotNull(needBraces, settings::setNeedBraces);
        copyIfNotNull(leftCurly, settings::setLeftCurly);
        copyIfNotNull(rightCurly, settings::setRightCurly);
        copyIfNotNull(emptyStatement, settings::setEmptyStatement);
        copyIfNotNull(equalsHashCode, settings::setEqualsHashCode);
        copyIfNotNull(illegalInstantiation, settings::setIllegalInstantiation);
        copyIfNotNull(missingSwitchDefault, settings::setMissingSwitchDefault);
        copyIfNotNull(simplifyBooleanExpression, settings::setSimplifyBooleanExpression);
        copyIfNotNull(simplifyBooleanReturn, settings::setSimplifyBooleanReturn);
        copyIfNotNull(finalClass, settings::setFinalClass);
        copyIfNotNull(hideUtilityClassConstructor, settings::setHideUtilityClassConstructor);
        copyIfNotNull(interfaceIsType, settings::setInterfaceIsType);
        copyIfNotNull(visibilityModifier, settings::setVisibilityModifier);
        copyIfNotNull(outerTypeFilename, settings::setOuterTypeFilename);
        copyIfNotNull(illegalTokenText, settings::setIllegalTokenText);
        copyIfNotNull(avoidEscapedUnicodeCharacters, settings::setAvoidEscapedUnicodeCharacters);
    }

    private static void copyIfNotNull(String value, Consumer<String> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private static void copyIfNotNull(Integer value, Consumer<Integer> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private static void copyIfNotNull(Boolean value, Consumer<Boolean> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}

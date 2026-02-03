package com.checkstylehub.analyzer.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing user-specific Checkstyle settings.
 * Each user has their own settings that persist across sessions.
 */
@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // General settings
    @Column(nullable = false)
    private String charset = "UTF-8";

    @Column(nullable = false)
    private String severity = "warning";

    @Column(nullable = false)
    private String fileExtensions = "java, properties, xml";

    @Column(nullable = false)
    private Integer lineLength = 120;

    @Column
    private String lineLengthIgnorePattern = "^package.*|^import.*|a href|href|http://|https://|ftp://";

    // Import rules
    @Column(nullable = false)
    private Boolean avoidStarImport = true;

    // Class design rules
    @Column(nullable = false)
    private Boolean oneTopLevelClass = true;

    @Column(nullable = false)
    private Boolean noLineWrap = true;

    // Block rules
    @Column(nullable = false)
    private Boolean emptyBlock = true;

    @Column(nullable = false)
    private Boolean needBraces = true;

    @Column(nullable = false)
    private Boolean leftCurly = true;

    @Column(nullable = false)
    private Boolean rightCurly = true;

    @Column(nullable = false)
    private Boolean emptyStatement = true;

    // Coding rules
    @Column(nullable = false)
    private Boolean equalsHashCode = true;

    @Column(nullable = false)
    private Boolean illegalInstantiation = true;

    @Column(nullable = false)
    private Boolean missingSwitchDefault = true;

    @Column(nullable = false)
    private Boolean simplifyBooleanExpression = true;

    @Column(nullable = false)
    private Boolean simplifyBooleanReturn = true;

    // Design rules
    @Column(nullable = false)
    private Boolean finalClass = true;

    @Column(nullable = false)
    private Boolean hideUtilityClassConstructor = true;

    @Column(nullable = false)
    private Boolean interfaceIsType = true;

    @Column(nullable = false)
    private Boolean visibilityModifier = true;

    // Miscellaneous rules
    @Column(nullable = false)
    private Boolean outerTypeFilename = true;

    @Column(nullable = false)
    private Boolean illegalTokenText = true;

    @Column(nullable = false)
    private Boolean avoidEscapedUnicodeCharacters = true;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public UserSettings() {
        this.updatedAt = LocalDateTime.now();
    }

    public UserSettings(User user) {
        this();
        this.user = user;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

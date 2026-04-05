package com.checkstylehub.analyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing user-specific Checkstyle settings.
 * Each user has their own settings that persist across sessions.
 */
@Entity
@Table(name = "user_settings")
@Getter
@Setter
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
}

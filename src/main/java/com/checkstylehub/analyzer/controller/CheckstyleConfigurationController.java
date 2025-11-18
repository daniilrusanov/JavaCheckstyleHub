package com.checkstylehub.analyzer.controller;

import com.checkstylehub.analyzer.dto.CheckstyleConfigurationDto;
import com.checkstylehub.analyzer.dto.CheckstyleRulesDto;
import com.checkstylehub.analyzer.dto.UpdateCheckstyleConfigurationDto;
import com.checkstylehub.analyzer.service.CheckstyleConfigurationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Checkstyle configuration.
 * Provides endpoints for getting, updating, and resetting configuration rules.
 */
@RestController
@RequestMapping("/api/checkstyle")
@CrossOrigin(origins = "*")
public class CheckstyleConfigurationController {

    private final CheckstyleConfigurationService configurationService;

    public CheckstyleConfigurationController(CheckstyleConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping("/configuration")
    public ResponseEntity<CheckstyleRulesDto> getActiveConfiguration() {
        CheckstyleRulesDto config = configurationService.getActiveConfigurationAsRules();
        return ResponseEntity.ok(config);
    }

    @PatchMapping("/configuration")
    public ResponseEntity<CheckstyleRulesDto> updateActiveConfiguration(
            @RequestBody CheckstyleRulesDto rulesDto) {
        CheckstyleRulesDto currentConfig = configurationService.getActiveConfigurationAsRules();
        mergeConfigurations(currentConfig, rulesDto);
        CheckstyleRulesDto updatedConfig = configurationService.updateActiveConfigurationFromRules(currentConfig);
        return ResponseEntity.ok(updatedConfig);
    }

    @PutMapping("/configuration")
    public ResponseEntity<CheckstyleRulesDto> replaceActiveConfiguration(
            @RequestBody CheckstyleRulesDto rulesDto) {
        CheckstyleRulesDto updatedConfig = configurationService.updateActiveConfigurationFromRules(rulesDto);
        return ResponseEntity.ok(updatedConfig);
    }

    /**
     * @deprecated Use structured configuration instead
     */
    @Deprecated
    @GetMapping("/configuration/xml")
    public ResponseEntity<CheckstyleConfigurationDto> getActiveConfigurationXml() {
        CheckstyleConfigurationDto config = configurationService.getActiveConfiguration();
        return ResponseEntity.ok(config);
    }

    @PostMapping("/configuration/xml")
    public ResponseEntity<CheckstyleRulesDto> updateFromXml(
            @RequestBody UpdateCheckstyleConfigurationDto updateDto) {
        configurationService.updateActiveConfiguration(updateDto);
        CheckstyleRulesDto config = configurationService.getActiveConfigurationAsRules();
        return ResponseEntity.ok(config);
    }

    @PostMapping("/configuration/reset")
    public ResponseEntity<CheckstyleRulesDto> resetToDefault() {
        CheckstyleRulesDto defaultConfig = new CheckstyleRulesDto();
        defaultConfig.setCharset("UTF-8");
        defaultConfig.setSeverity("warning");
        defaultConfig.setFileExtensions("java, properties, xml");
        defaultConfig.setLineLength(120);
        defaultConfig.setLineLengthIgnorePattern("^package.*|^import.*|a href|href|http://|https://|ftp://");

        defaultConfig.setAvoidStarImport(true);
        defaultConfig.setOneTopLevelClass(true);
        defaultConfig.setNoLineWrap(true);
        defaultConfig.setEmptyBlock(true);
        defaultConfig.setNeedBraces(true);
        defaultConfig.setLeftCurly(true);
        defaultConfig.setRightCurly(true);
        defaultConfig.setEmptyStatement(true);
        defaultConfig.setEqualsHashCode(true);
        defaultConfig.setIllegalInstantiation(true);
        defaultConfig.setMissingSwitchDefault(true);
        defaultConfig.setSimplifyBooleanExpression(true);
        defaultConfig.setSimplifyBooleanReturn(true);
        defaultConfig.setFinalClass(true);
        defaultConfig.setHideUtilityClassConstructor(true);
        defaultConfig.setInterfaceIsType(true);
        defaultConfig.setVisibilityModifier(true);
        defaultConfig.setOuterTypeFilename(true);
        defaultConfig.setIllegalTokenText(true);
        defaultConfig.setAvoidEscapedUnicodeCharacters(true);

        CheckstyleRulesDto updatedConfig = configurationService.updateActiveConfigurationFromRules(defaultConfig);
        return ResponseEntity.ok(updatedConfig);
    }

    private void mergeConfigurations(CheckstyleRulesDto target, CheckstyleRulesDto source) {
        if (source.getCharset() != null) target.setCharset(source.getCharset());
        if (source.getSeverity() != null) target.setSeverity(source.getSeverity());
        if (source.getFileExtensions() != null) target.setFileExtensions(source.getFileExtensions());
        if (source.getLineLength() != null) target.setLineLength(source.getLineLength());
        if (source.getLineLengthIgnorePattern() != null)
            target.setLineLengthIgnorePattern(source.getLineLengthIgnorePattern());
        if (source.getAvoidStarImport() != null) target.setAvoidStarImport(source.getAvoidStarImport());
        if (source.getOneTopLevelClass() != null) target.setOneTopLevelClass(source.getOneTopLevelClass());
        if (source.getNoLineWrap() != null) target.setNoLineWrap(source.getNoLineWrap());
        if (source.getEmptyBlock() != null) target.setEmptyBlock(source.getEmptyBlock());
        if (source.getNeedBraces() != null) target.setNeedBraces(source.getNeedBraces());
        if (source.getLeftCurly() != null) target.setLeftCurly(source.getLeftCurly());
        if (source.getRightCurly() != null) target.setRightCurly(source.getRightCurly());
        if (source.getEmptyStatement() != null) target.setEmptyStatement(source.getEmptyStatement());
        if (source.getEqualsHashCode() != null) target.setEqualsHashCode(source.getEqualsHashCode());
        if (source.getIllegalInstantiation() != null) target.setIllegalInstantiation(source.getIllegalInstantiation());
        if (source.getMissingSwitchDefault() != null) target.setMissingSwitchDefault(source.getMissingSwitchDefault());
        if (source.getSimplifyBooleanExpression() != null)
            target.setSimplifyBooleanExpression(source.getSimplifyBooleanExpression());
        if (source.getSimplifyBooleanReturn() != null)
            target.setSimplifyBooleanReturn(source.getSimplifyBooleanReturn());
        if (source.getFinalClass() != null) target.setFinalClass(source.getFinalClass());
        if (source.getHideUtilityClassConstructor() != null)
            target.setHideUtilityClassConstructor(source.getHideUtilityClassConstructor());
        if (source.getInterfaceIsType() != null) target.setInterfaceIsType(source.getInterfaceIsType());
        if (source.getVisibilityModifier() != null) target.setVisibilityModifier(source.getVisibilityModifier());
        if (source.getOuterTypeFilename() != null) target.setOuterTypeFilename(source.getOuterTypeFilename());
        if (source.getIllegalTokenText() != null) target.setIllegalTokenText(source.getIllegalTokenText());
        if (source.getAvoidEscapedUnicodeCharacters() != null)
            target.setAvoidEscapedUnicodeCharacters(source.getAvoidEscapedUnicodeCharacters());
    }
}



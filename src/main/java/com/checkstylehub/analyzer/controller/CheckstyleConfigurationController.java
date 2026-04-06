package com.checkstylehub.analyzer.controller;

import com.checkstylehub.analyzer.dto.CheckstyleConfigurationDto;
import com.checkstylehub.analyzer.dto.CheckstyleRulesDto;
import com.checkstylehub.analyzer.dto.UpdateCheckstyleConfigurationDto;
import com.checkstylehub.analyzer.service.CheckstyleConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * REST controller for managing Checkstyle configuration.
 * Provides endpoints for getting, updating, and resetting configuration rules.
 */
@RestController
@RequestMapping("/api/checkstyle")
@RequiredArgsConstructor
public class CheckstyleConfigurationController {

    private final CheckstyleConfigurationService configurationService;

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
        copyIfNotNull(source, target, CheckstyleRulesDto::getCharset, CheckstyleRulesDto::setCharset);
        copyIfNotNull(source, target, CheckstyleRulesDto::getSeverity, CheckstyleRulesDto::setSeverity);
        copyIfNotNull(source, target, CheckstyleRulesDto::getFileExtensions, CheckstyleRulesDto::setFileExtensions);
        copyIfNotNull(source, target, CheckstyleRulesDto::getLineLength, CheckstyleRulesDto::setLineLength);
        copyIfNotNull(source, target, CheckstyleRulesDto::getLineLengthIgnorePattern, CheckstyleRulesDto::setLineLengthIgnorePattern);
        copyIfNotNull(source, target, CheckstyleRulesDto::getAvoidStarImport, CheckstyleRulesDto::setAvoidStarImport);
        copyIfNotNull(source, target, CheckstyleRulesDto::getOneTopLevelClass, CheckstyleRulesDto::setOneTopLevelClass);
        copyIfNotNull(source, target, CheckstyleRulesDto::getNoLineWrap, CheckstyleRulesDto::setNoLineWrap);
        copyIfNotNull(source, target, CheckstyleRulesDto::getEmptyBlock, CheckstyleRulesDto::setEmptyBlock);
        copyIfNotNull(source, target, CheckstyleRulesDto::getNeedBraces, CheckstyleRulesDto::setNeedBraces);
        copyIfNotNull(source, target, CheckstyleRulesDto::getLeftCurly, CheckstyleRulesDto::setLeftCurly);
        copyIfNotNull(source, target, CheckstyleRulesDto::getRightCurly, CheckstyleRulesDto::setRightCurly);
        copyIfNotNull(source, target, CheckstyleRulesDto::getEmptyStatement, CheckstyleRulesDto::setEmptyStatement);
        copyIfNotNull(source, target, CheckstyleRulesDto::getEqualsHashCode, CheckstyleRulesDto::setEqualsHashCode);
        copyIfNotNull(source, target, CheckstyleRulesDto::getIllegalInstantiation, CheckstyleRulesDto::setIllegalInstantiation);
        copyIfNotNull(source, target, CheckstyleRulesDto::getMissingSwitchDefault, CheckstyleRulesDto::setMissingSwitchDefault);
        copyIfNotNull(source, target, CheckstyleRulesDto::getSimplifyBooleanExpression, CheckstyleRulesDto::setSimplifyBooleanExpression);
        copyIfNotNull(source, target, CheckstyleRulesDto::getSimplifyBooleanReturn, CheckstyleRulesDto::setSimplifyBooleanReturn);
        copyIfNotNull(source, target, CheckstyleRulesDto::getFinalClass, CheckstyleRulesDto::setFinalClass);
        copyIfNotNull(source, target, CheckstyleRulesDto::getHideUtilityClassConstructor, CheckstyleRulesDto::setHideUtilityClassConstructor);
        copyIfNotNull(source, target, CheckstyleRulesDto::getInterfaceIsType, CheckstyleRulesDto::setInterfaceIsType);
        copyIfNotNull(source, target, CheckstyleRulesDto::getVisibilityModifier, CheckstyleRulesDto::setVisibilityModifier);
        copyIfNotNull(source, target, CheckstyleRulesDto::getOuterTypeFilename, CheckstyleRulesDto::setOuterTypeFilename);
        copyIfNotNull(source, target, CheckstyleRulesDto::getIllegalTokenText, CheckstyleRulesDto::setIllegalTokenText);
        copyIfNotNull(source, target, CheckstyleRulesDto::getAvoidEscapedUnicodeCharacters, CheckstyleRulesDto::setAvoidEscapedUnicodeCharacters);
    }

    private static <T> void copyIfNotNull(
            CheckstyleRulesDto source,
            CheckstyleRulesDto target,
            Function<CheckstyleRulesDto, T> getter,
            BiConsumer<CheckstyleRulesDto, T> setter) {
        T value = getter.apply(source);
        if (value != null) {
            setter.accept(target, value);
        }
    }
}



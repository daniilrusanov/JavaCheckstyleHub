package com.checkstylehub.analyzer.service;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs PMD programmatically on a set of Java sources using the error-prone and design category rulesets
 * (including cyclomatic and cognitive complexity).
 */
@Service
public class PmdService {

    public static final String DEFAULT_RULESET = "category/java/errorprone.xml";

    /**
     * @param baseDir   repository root (used to relativize paths in reports)
     * @param javaFiles absolute or relative paths to {@code .java} files
     * @return violations with absolute file paths (for consistent relativization by callers)
     */
    public List<PmdViolation> runPmd(Path baseDir, List<Path> javaFiles) throws Exception {
        if (javaFiles.isEmpty()) {
            return List.of();
        }
        PMDConfiguration config = new PMDConfiguration();
        Language javaLang = LanguageRegistry.PMD.getLanguageById("java");
        config.setDefaultLanguageVersion(javaLang.getLatestVersion());
        config.setSourceEncoding(StandardCharsets.UTF_8);
        config.collectFilesRecursively(false);
        config.addRuleSet(DEFAULT_RULESET);
        config.addRuleSet("category/java/design.xml");
        Path basePath = baseDir.toAbsolutePath().normalize();
        config.addRelativizeRoot(basePath);
        config.setInputPathList(javaFiles.stream()
                .map(p -> p.toAbsolutePath().normalize())
                .collect(Collectors.toList()));

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            Report report = pmd.performAnalysisAndCollectReport();
            if (!report.getConfigurationErrors().isEmpty()) {
                String msg = report.getConfigurationErrors().stream()
                        .map(e -> e.rule().getName() + ": " + e.issue())
                        .collect(Collectors.joining("; "));
                System.err.println("PMD configuration warnings (skipped rules): " + msg);
            }
            if (!report.getProcessingErrors().isEmpty()) {
                String msg = report.getProcessingErrors().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining("; "));
                throw new IllegalStateException("PMD processing errors: " + msg);
            }
            List<PmdViolation> out = new ArrayList<>();
            for (RuleViolation v : report.getViolations()) {
                String absolutePath = v.getFileId().getAbsolutePath();
                out.add(new PmdViolation(
                        absolutePath,
                        v.getBeginLine(),
                        v.getRule().getPriority().name(),
                        v.getDescription()
                ));
            }
            return out;
        }
    }

    public record PmdViolation(String absoluteFilePath, int line, String severity, String message) {
    }
}

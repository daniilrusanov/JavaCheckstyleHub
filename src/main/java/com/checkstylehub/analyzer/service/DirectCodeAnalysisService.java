package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.AnalysisResultDto;
import com.checkstylehub.analyzer.dto.CodeAnalysisResponseDto;
import com.checkstylehub.analyzer.dto.CodeAnalysisResponseDto.CompilationError;
import com.checkstylehub.analyzer.entity.AnalyzerType;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for analyzing Java code submitted directly (not from a repository).
 * Supports single file analysis with optional compilation checking.
 */
@Service
@RequiredArgsConstructor
public class DirectCodeAnalysisService {

    private final CheckstyleService checkstyleService;
    private final PmdService pmdService;
    private final MetricsCalculationService metricsCalculationService;

    // Pattern to extract class name from Java code
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile(
        "(?:public\\s+)?(?:abstract\\s+)?(?:final\\s+)?class\\s+(\\w+)"
    );

    /**
     * Analyzes Java code directly without requiring a Git repository.
     *
     * @param code             the Java source code to analyze
     * @param fileName         optional filename (will be derived from class name if null)
     * @param checkCompilation whether to check if the code compiles
     * @param customConfigXml  optional custom Checkstyle configuration
     * @return analysis results including violations and optional compilation status
     */
    public CodeAnalysisResponseDto analyzeCode(String code, String fileName,
            boolean checkCompilation, String customConfigXml) {

        if (code == null || code.trim().isEmpty()) {
            return CodeAnalysisResponseDto.error("Код не може бути порожнім");
        }

        Path tempDir = null;
        try {
            // Create temporary directory
            tempDir = Files.createTempDirectory("checkstyle-direct-");

            // Determine filename from class name or use provided/default
            String actualFileName = determineFileName(code, fileName);

            // Write code to temporary file
            Path javaFile = tempDir.resolve(actualFileName);
            Files.writeString(javaFile, code);

            CodeAnalysisResponseDto response = new CodeAnalysisResponseDto();
            response.setSuccess(true);

            // Check compilation if requested
            if (checkCompilation) {
                List<CompilationError> compilationErrors = checkCompilation(javaFile, code);
                response.setCompilationSuccess(compilationErrors.isEmpty());
                response.setCompilationErrors(compilationErrors);
            }

            // Run Checkstyle + PMD analysis
            List<Path> javaFiles = Collections.singletonList(javaFile);
            List<AnalysisResultDto> violationDtos = new ArrayList<>();
            List<AuditEvent> checkstyleEvents = new ArrayList<>();
            List<PmdService.PmdViolation> pmdViolations = new ArrayList<>();
            boolean checkstyleCompleted = false;
            boolean syntheticCheckstyleViolation = false;

            try {
                checkstyleEvents = checkstyleService.runCheckstyle(
                    tempDir, javaFiles, customConfigXml
                );

                for (AuditEvent event : checkstyleEvents) {
                    violationDtos.add(new AnalysisResultDto(
                        null,
                        actualFileName,
                        event.getLine(),
                        event.getSeverityLevel().getName(),
                        event.getMessage(),
                        AnalyzerType.CHECKSTYLE,
                        null
                    ));
                }
                checkstyleCompleted = true;
            } catch (Exception checkstyleError) {
                if (response.getCompilationSuccess() != null && !response.getCompilationSuccess()) {
                    try {
                        long loc = Math.max(1, metricsCalculationService.countLinesOfCode(javaFiles));
                        int qs = metricsCalculationService.computeQualityScore(
                                Collections.emptyList(),
                                Collections.emptyList(),
                                loc,
                                true);
                        response.setViolations(violationDtos);
                        response.setViolationCount(violationDtos.size());
                        response.setQualityScore(qs);
                    } catch (IOException e) {
                        response.setQualityScore(0);
                    }
                    return response;
                }
                syntheticCheckstyleViolation = true;
                violationDtos.add(new AnalysisResultDto(
                    null,
                    actualFileName,
                    1,
                    "ERROR",
                    "Код містить критичні синтаксичні помилки і не може бути проаналізований",
                    AnalyzerType.CHECKSTYLE,
                    null
                ));
            }

            if (checkstyleCompleted) {
                try {
                    pmdViolations = pmdService.runPmd(tempDir, javaFiles);
                    for (PmdService.PmdViolation v : pmdViolations) {
                        violationDtos.add(new AnalysisResultDto(
                                null,
                                actualFileName,
                                v.line(),
                                v.severity(),
                                v.message(),
                                AnalyzerType.PMD,
                                null
                        ));
                    }
                } catch (Exception pmdError) {
                    System.err.println("PMD Error in Direct Analysis: " + pmdError.getMessage());
                }
            }

            response.setViolations(violationDtos);
            response.setViolationCount(violationDtos.size());

            try {
                long loc = Math.max(1, metricsCalculationService.countLinesOfCode(javaFiles));
                boolean compilationFailed =
                        response.getCompilationSuccess() != null && !response.getCompilationSuccess();
                long tdi = metricsCalculationService.computeTotalDefectIndex(
                        checkstyleEvents, pmdViolations, compilationFailed);
                if (syntheticCheckstyleViolation) {
                    tdi += 5;
                }
                double dd = metricsCalculationService.computeDefectDensity(tdi, loc);
                int qualityScore = metricsCalculationService.computeQualityScoreFromDefectDensity(dd);
                response.setQualityScore(qualityScore);
            } catch (IOException e) {
                response.setQualityScore(0);
            }

            return response;

        } catch (Exception e) {
            // Handle truly unexpected errors gracefully
            String userMessage = "Не вдалося проаналізувати код. ";
            if (e.getMessage() != null && e.getMessage().contains("Exception was thrown while processing")) {
                userMessage += "Перевірте синтаксис коду - можливо, він містить критичні помилки.";
            } else {
                userMessage += "Спробуйте ще раз або перевірте синтаксис коду.";
            }
            return CodeAnalysisResponseDto.error(userMessage);
        } finally {
            // Cleanup temporary directory
            if (tempDir != null) {
                try {
                    deleteDirectory(tempDir);
                } catch (IOException ignored) { }
            }
        }
    }

    /**
     * Determines the appropriate filename based on the class name in the code.
     */
    private String determineFileName(String code, String providedFileName) {
        if (providedFileName != null && !providedFileName.trim().isEmpty()) {
            // Ensure .java extension
            if (!providedFileName.endsWith(".java")) {
                return providedFileName + ".java";
            }
            return providedFileName;
        }

        // Try to extract class name from code
        Matcher matcher = CLASS_NAME_PATTERN.matcher(code);
        if (matcher.find()) {
            return matcher.group(1) + ".java";
        }

        // Default filename
        return "Main.java";
    }

    /**
     * Checks if the Java code compiles successfully using the Java Compiler API.
     *
     * @param javaFile path to the Java file
     * @param code     the source code
     * @return list of compilation errors (empty if compilation successful)
     */
    private List<CompilationError> checkCompilation(Path javaFile, String code) {
        List<CompilationError> errors = new ArrayList<>();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            // JDK not available (running in JRE)
            errors.add(new CompilationError(0, 0,
                "Компілятор Java недоступний (потрібен JDK)", "ERROR"));
            return errors;
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(
            diagnostics, Locale.getDefault(), null
        );

        try {
            Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjects(javaFile.toFile());

            StringWriter output = new StringWriter();
            JavaCompiler.CompilationTask task = compiler.getTask(
                output, fileManager, diagnostics, null, null, compilationUnits
            );

            boolean success = task.call();

            if (!success) {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        errors.add(new CompilationError(
                            diagnostic.getLineNumber(),
                            diagnostic.getColumnNumber(),
                            diagnostic.getMessage(Locale.getDefault()),
                            diagnostic.getKind().name()
                        ));
                    }
                }
            }

        } catch (Exception e) {
            errors.add(new CompilationError(0, 0,
                "Помилка компіляції: " + e.getMessage(), "ERROR"));
        } finally {
            try {
                fileManager.close();
            } catch (IOException ignored) { }
        }

        return errors;
    }

    /**
     * Recursively deletes a directory.
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a)) // Reverse order to delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException ignored) { }
                });
        }
    }
}

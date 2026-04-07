package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.AnalysisResultDto;
import com.checkstylehub.analyzer.dto.CodeAnalysisResponseDto;
import com.checkstylehub.analyzer.dto.CodeAnalysisResponseDto.CompilationError;
import com.checkstylehub.analyzer.entity.AnalyzerType;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for analyzing Java code submitted directly (not from a repository).
 * Supports single file analysis with optional compilation checking.
 */
@Service
@RequiredArgsConstructor
public class DirectCodeAnalysisService {

    private static final String SYNTAX_ERROR_CHECKSTYLE_MSG =
            "Код містить критичні синтаксичні помилки і не може бути проаналізований";
    private static final String PROCESSING_EXCEPTION_FRAGMENT = "Exception was thrown while processing";
    private static final String GENERIC_FAILURE_UA = "Не вдалося проаналізувати код. ";
    private static final String SYNTAX_HINT_UA =
            "Перевірте синтаксис коду - можливо, він містить критичні помилки.";
    private static final String RETRY_HINT_UA = "Спробуйте ще раз або перевірте синтаксис коду.";

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
     * @param fileName         optional filename (will be derived from the class name if null)
     * @param checkCompilation whether to check if the code compiles
     * @param customConfigXml  optional custom Checkstyle configuration
     * @return analysis results including violations and optional compilation status
     */
    public CodeAnalysisResponseDto analyzeCode(String code, String fileName,
            boolean checkCompilation, String customConfigXml) {

        if (code == null || code.trim().isEmpty()) {
            return CodeAnalysisResponseDto.error("Код не може бути порожнім");
        }

        Optional<Path> tempDirHolder = Optional.empty();
        try {
            Path tempDir = Files.createTempDirectory("checkstyle-direct-");
            tempDirHolder = Optional.of(tempDir);
            return runAnalysisInTempDir(tempDir, code, fileName, checkCompilation, customConfigXml);
        } catch (IOException | IllegalStateException e) {
            return buildUnexpectedFailureResponse(e);
        } finally {
            tempDirHolder.ifPresent(this::deleteDirectoryQuietly);
        }
    }

    private CodeAnalysisResponseDto buildUnexpectedFailureResponse(Exception e) {
        String userMessage = GENERIC_FAILURE_UA;
        if (e.getMessage() != null && e.getMessage().contains(PROCESSING_EXCEPTION_FRAGMENT)) {
            userMessage += SYNTAX_HINT_UA;
        } else {
            userMessage += RETRY_HINT_UA;
        }
        return CodeAnalysisResponseDto.error(userMessage);
    }

    private void deleteDirectoryQuietly(Path directory) {
        try {
            deleteDirectory(directory);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    private CodeAnalysisResponseDto runAnalysisInTempDir(Path tempDir, String code, String fileName,
            boolean checkCompilation, String customConfigXml) throws IOException {

        String actualFileName = determineFileName(code, fileName);
        Path javaFile = tempDir.resolve(actualFileName);
        Files.writeString(javaFile, code);

        CodeAnalysisResponseDto response = new CodeAnalysisResponseDto();
        response.setSuccessful(true);

        if (checkCompilation) {
            List<CompilationError> compilationErrors = checkCompilation(javaFile);
            response.setCompilationSuccess(compilationErrors.isEmpty());
            response.setCompilationErrors(compilationErrors);
        }

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
        } catch (CheckstyleException checkstyleError) {
            if (response.getCompilationSuccess() != null && !response.getCompilationSuccess()) {
                return buildResponseWhenCompilationFailed(response, violationDtos, javaFiles);
            }
            syntheticCheckstyleViolation = true;
            violationDtos.add(new AnalysisResultDto(
                null,
                actualFileName,
                1,
                "ERROR",
                SYNTAX_ERROR_CHECKSTYLE_MSG,
                AnalyzerType.CHECKSTYLE,
                null
            ));
        }

        if (checkstyleCompleted) {
            pmdViolations = runPmdWithLogging(tempDir, javaFiles, actualFileName, violationDtos);
        }

        response.setViolations(violationDtos);
        response.setViolationCount(violationDtos.size());

        applyQualityScore(response, checkstyleEvents, pmdViolations, syntheticCheckstyleViolation, javaFiles);

        return response;
    }

    private CodeAnalysisResponseDto buildResponseWhenCompilationFailed(CodeAnalysisResponseDto response,
            List<AnalysisResultDto> violationDtos, List<Path> javaFiles) {
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

    private List<PmdService.PmdViolation> runPmdWithLogging(Path tempDir, List<Path> javaFiles,
            String actualFileName, List<AnalysisResultDto> violationDtos) {
        try {
            List<PmdService.PmdViolation> pmdViolations = pmdService.runPmd(tempDir, javaFiles);
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
            return pmdViolations;
        } catch (IllegalStateException pmdError) {
            System.err.println("PMD Error in Direct Analysis: " + pmdError.getMessage());
            return new ArrayList<>();
        }
    }

    private void applyQualityScore(CodeAnalysisResponseDto response,
            List<AuditEvent> checkstyleEvents,
            List<PmdService.PmdViolation> pmdViolations,
            boolean syntheticCheckstyleViolation,
            List<Path> javaFiles) {
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
    }

    /**
     * Determines the appropriate filename based on the class name in the code.
     */
    private String determineFileName(String code, String providedFileName) {
        if (providedFileName != null && !providedFileName.trim().isEmpty()) {
            if (!providedFileName.endsWith(".java")) {
                return providedFileName + ".java";
            }
            return providedFileName;
        }

        Matcher matcher = CLASS_NAME_PATTERN.matcher(code);
        if (matcher.find()) {
            return matcher.group(1) + ".java";
        }

        return "Main.java";
    }

    /**
     * Checks if the Java code compiles successfully using the Java Compiler API.
     *
     * @param javaFile path to the Java file
     * @return list of compilation errors (empty if compilation successful)
     */
    @SuppressWarnings("PMD.UnusedFormalParameter")
    private List<CompilationError> checkCompilation(Path javaFile) {
        List<CompilationError> errors = new ArrayList<>();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            errors.add(new CompilationError(0, 0,
                "Компілятор Java недоступний (потрібен JDK)", "ERROR"));
            return errors;
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnostics, Locale.getDefault(), null
        )) {
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

        } catch (IOException e) {
            errors.add(new CompilationError(0, 0,
                    "Помилка компіляції: " + e.getMessage(), "ERROR"));
        }

        return errors;
    }

    /**
     * Recursively deletes a directory.
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException ignored) {
                        // ignored
                    }
                });
        }
    }
}

package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.AnalysisResultDto;
import com.checkstylehub.analyzer.dto.CodeAnalysisResponseDto;
import com.checkstylehub.analyzer.dto.CodeAnalysisResponseDto.CompilationError;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
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
public class DirectCodeAnalysisService {

    private final CheckstyleService checkstyleService;

    // Pattern to extract class name from Java code
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile(
        "(?:public\\s+)?(?:abstract\\s+)?(?:final\\s+)?class\\s+(\\w+)"
    );

    public DirectCodeAnalysisService(CheckstyleService checkstyleService) {
        this.checkstyleService = checkstyleService;
    }

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

            // Run Checkstyle analysis
            List<Path> javaFiles = Collections.singletonList(javaFile);
            List<AnalysisResultDto> violationDtos = new ArrayList<>();

            try {
                List<AuditEvent> violations = checkstyleService.runCheckstyle(
                    tempDir, javaFiles, customConfigXml
                );

                // Convert to DTOs
                for (AuditEvent event : violations) {
                    violationDtos.add(new AnalysisResultDto(
                        null,
                        actualFileName,
                        event.getLine(),
                        event.getSeverityLevel().getName(),
                        event.getMessage()
                    ));
                }
            } catch (Exception checkstyleError) {
                // Checkstyle failed - likely due to severe syntax errors
                // Still return a valid response with compilation errors info
                if (response.getCompilationSuccess() != null && !response.getCompilationSuccess()) {
                    // Code doesn't compile, that's the main issue
                    response.setViolations(violationDtos);
                    response.setViolationCount(0);
                    response.setQualityScore(0);
                    return response;
                }
                // Add a synthetic error for unparseable code
                violationDtos.add(new AnalysisResultDto(
                    null,
                    actualFileName,
                    1,
                    "ERROR",
                    "Код містить критичні синтаксичні помилки і не може бути проаналізований"
                ));
            }

            response.setViolations(violationDtos);
            response.setViolationCount(violationDtos.size());

            // Calculate quality score
            int qualityScore = calculateQualityScore(code, violationDtos.size(),
                response.getCompilationSuccess());
            response.setQualityScore(qualityScore);

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
     * Calculates a quality score (0-100) based on various factors.
     *
     * @param code               the source code
     * @param violationCount     number of Checkstyle violations
     * @param compilationSuccess whether the code compiles
     * @return quality score from 0 to 100
     */
    private int calculateQualityScore(String code, int violationCount, Boolean compilationSuccess) {
        // Start with 100
        int score = 100;

        // If compilation failed, cap at 50
        if (compilationSuccess != null && !compilationSuccess) {
            score = Math.min(score, 50);
        }

        // Count lines of code (approximate)
        int lineCount = code.split("\n").length;

        // Calculate violations per line ratio
        double violationsPerLine = lineCount > 0 ? (double) violationCount / lineCount : 0;

        // Deduct points based on violations per line
        // More than 0.5 violations per line is very bad
        if (violationsPerLine > 0.5) {
            score -= 40;
        } else if (violationsPerLine > 0.3) {
            score -= 30;
        } else if (violationsPerLine > 0.2) {
            score -= 20;
        } else if (violationsPerLine > 0.1) {
            score -= 10;
        } else if (violationsPerLine > 0.05) {
            score -= 5;
        }

        // Additional penalty for absolute number of violations
        if (violationCount > 20) {
            score -= 15;
        } else if (violationCount > 10) {
            score -= 10;
        } else if (violationCount > 5) {
            score -= 5;
        }

        return Math.max(0, Math.min(100, score));
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

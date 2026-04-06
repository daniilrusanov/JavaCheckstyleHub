package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.CachedAnalysisBundle;
import com.checkstylehub.analyzer.dto.LogMessageDto;
import com.checkstylehub.analyzer.entity.AnalysisRequest;
import com.checkstylehub.analyzer.entity.AnalysisResult;
import com.checkstylehub.analyzer.entity.AnalyzerType;
import com.checkstylehub.analyzer.exception.RepositoryAccessException;
import com.checkstylehub.analyzer.repository.AnalysisRequestRepository;
import com.checkstylehub.analyzer.repository.AnalysisResultRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for orchestrating the complete code analysis workflow.
 * Handles repository cloning, Checkstyle and PMD execution, result persistence, and logging.
 * When RabbitMQ is enabled, work is consumed from a queue; otherwise the controller runs this on a task executor.
 */
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final GitService gitService;
    private final CheckstyleService checkstyleService;
    private final PmdService pmdService;
    private final MetricsCalculationService metricsCalculationService;
    private final AnalysisRequestRepository requestRepository;
    private final AnalysisResultRepository resultRepository;
    private final com.checkstylehub.analyzer.repository.AnalysisLogRepository logRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EntityManager entityManager;
    @Qualifier("analysisResultsRedisTemplate")
    private final RedisTemplate<String, CachedAnalysisBundle> redisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final CheckstyleConfigurationService configurationService;

    /**
     * Executes the complete analysis workflow.
     * Steps: clone repository → find Java files → run Checkstyle and PMD in parallel → save results.
     * Status updates and logs are sent via WebSocket in real-time.
     *
     * @param requestId              the ID of the analysis request
     * @param customCheckstyleConfig optional custom Checkstyle XML configuration
     */
    public void startAnalysisFlow(Long requestId, String customCheckstyleConfig) {
        String logTopic = "/topic/logs/" + requestId;
        Path tempDir = null;
        String cacheKey;
        CachedAnalysisBundle resultsForCache;

        try {
            String repoUrl = transactionTemplate.execute(status ->
                    requestRepository.findById(requestId)
                            .map(AnalysisRequest::getRepoUrl)
                            .orElseThrow(() -> new RuntimeException("Request not found")));

            logInfo("Перевіряю кеш та отримую останній коміт з віддаленого репозиторію...", logTopic);
            String commitHash = gitService.getLatestCommitHash(repoUrl);

            String effectiveConfig = (customCheckstyleConfig != null && !customCheckstyleConfig.isBlank())
                    ? customCheckstyleConfig
                    : configurationService.getActiveConfigurationXml();
            String configHash = org.springframework.util.DigestUtils.md5DigestAsHex(
                    effectiveConfig.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            cacheKey = repoUrl + "_" + commitHash + "_" + configHash;

            CachedAnalysisBundle cached;
            try {
                cached = redisTemplate.opsForValue().get(cacheKey);
            } catch (Exception ignored) {
                cached = null;
            }
            if (cached != null && cached.getResults() != null) {
                applyCachedResults(requestId, cached, logTopic);
                return;
            }

            updateStatusAndLog(requestId, AnalysisRequest.RequestStatus.CLONING, "Починаю клонування...", logTopic);
            tempDir = gitService.cloneRepository(repoUrl);

            logInfo("Клонування завершено. Шукаю Java файли...", logTopic);
            List<Path> javaFiles = checkstyleService.findJavaFiles(tempDir);
            if (javaFiles.isEmpty()) {
                throw new IllegalStateException("Репозиторій не містить файлів Java (.java). Аналіз неможливий.");
            }
            logInfo("Знайдено " + javaFiles.size() + " Java файлів. Запускаю аналіз...", logTopic);

            long linesOfCode;
            try {
                linesOfCode = metricsCalculationService.countLinesOfCode(javaFiles);
            } catch (IOException e) {
                throw new IllegalStateException("Не вдалося підрахувати рядки коду (LOC): " + e.getMessage(), e);
            }

            updateStatusAndLog(requestId, AnalysisRequest.RequestStatus.ANALYZING,
                    "Запуск Checkstyle та PMD паралельно (ruleset " + PmdService.DEFAULT_RULESET + ")...", logTopic);

            final Path analysisRoot = tempDir;
            final List<Path> analysisJavaFiles = javaFiles;
            final String analysisCheckstyleConfig = customCheckstyleConfig;

            CompletableFuture<List<com.puppycrawl.tools.checkstyle.api.AuditEvent>> checkstyleFuture =
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            return checkstyleService.runCheckstyle(
                                    analysisRoot, analysisJavaFiles, analysisCheckstyleConfig);
                        } catch (Exception e) {
                            logInfo("Checkstyle завершився з помилкою: " + e.getMessage(), logTopic);
                            return new ArrayList<>();
                        }
                    });

            CompletableFuture<List<PmdService.PmdViolation>> pmdFuture =
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            return pmdService.runPmd(analysisRoot, analysisJavaFiles);
                        } catch (Exception e) {
                            logInfo("PMD завершився з помилкою: " + e.getMessage(), logTopic);
                            return new ArrayList<>();
                        }
                    });

            CompletableFuture.allOf(checkstyleFuture, pmdFuture).join();

            List<com.puppycrawl.tools.checkstyle.api.AuditEvent> checkstyleViolations = checkstyleFuture.join();
            List<PmdService.PmdViolation> pmdViolations = pmdFuture.join();

            logInfo("Checkstyle завершено. Знайдено " + checkstyleViolations.size() + " порушень.", logTopic);
            logInfo("PMD завершено. Знайдено " + pmdViolations.size() + " порушень.", logTopic);

            int totalViolations = checkstyleViolations.size() + pmdViolations.size();
            logInfo("Збереження " + totalViolations + " результатів...", logTopic);

            final Path cloneRoot = tempDir;
            final List<com.puppycrawl.tools.checkstyle.api.AuditEvent> csEvents = checkstyleViolations;
            final List<PmdService.PmdViolation> pmdEvents = pmdViolations;
            final long loc = linesOfCode;

            resultsForCache = transactionTemplate.execute(status -> {
                AnalysisRequest request = requestRepository.findById(requestId)
                        .orElseThrow(() -> new RuntimeException("Request not found"));
                request = entityManager.merge(request);

                int qualityScore = metricsCalculationService.computeQualityScore(
                        csEvents, pmdEvents, loc, false);
                request.setQualityScore(qualityScore);
                requestRepository.save(request);

                CachedAnalysisBundle bundle = new CachedAnalysisBundle();
                bundle.setLinesOfCode(loc);
                bundle.setQualityScore(qualityScore);
                List<AnalysisResult> cachedResults = new ArrayList<>();
                for (com.puppycrawl.tools.checkstyle.api.AuditEvent event : csEvents) {
                    AnalysisResult result = new AnalysisResult();
                    result.setRequest(request);
                    result.setAnalyzerType(AnalyzerType.CHECKSTYLE);
                    String relativePath = safeRelativizeToString(cloneRoot, Path.of(event.getFileName()));
                    result.setFilePath(relativePath);
                    result.setLineNumber(event.getLine());
                    result.setSeverity(event.getSeverityLevel().getName());
                    result.setMessage(event.getMessage());
                    result.setCodeSnippet(extractCodeSnippet(event.getFileName(), event.getLine()));
                    resultRepository.save(result);
                    cachedResults.add(result);
                }
                for (PmdService.PmdViolation v : pmdEvents) {
                    AnalysisResult result = new AnalysisResult();
                    result.setRequest(request);
                    result.setAnalyzerType(AnalyzerType.PMD);
                    result.setFilePath(safeRelativizeToString(cloneRoot, Path.of(v.absoluteFilePath())));
                    result.setLineNumber(v.line());
                    result.setSeverity(v.severity());
                    result.setMessage(v.message());
                    result.setCodeSnippet(extractCodeSnippet(v.absoluteFilePath(), v.line()));
                    resultRepository.save(result);
                    cachedResults.add(result);
                }
                bundle.setResults(cachedResults);

                entityManager.flush();
                return bundle;
            });

            int qualityScore = resultsForCache != null && resultsForCache.getQualityScore() != null
                    ? resultsForCache.getQualityScore()
                    : 0;
            logInfo("Результати успішно збережено в базу даних.", logTopic);
            logInfo("Показник якості (Quality Score): " + qualityScore + "/100", logTopic);

            if (resultsForCache != null) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, resultsForCache, Duration.ofDays(7));
                } catch (Exception e) {
                    logInfo("Кеш Redis недоступний, результати лише в БД: " + e.getMessage(), logTopic);
                }
            }

            updateStatusAndLog(requestId, AnalysisRequest.RequestStatus.COMPLETED,
                    "Аналіз завершено. Знайдено " + totalViolations + " порушень (Checkstyle: "
                            + checkstyleViolations.size() + ", PMD: " + pmdViolations.size() + ").", logTopic);

        } catch (RepositoryAccessException | IllegalStateException | InterruptedException e) {
            handleFailure(requestId, e.getMessage(), logTopic);
        } catch (Exception e) {
            e.printStackTrace();
            handleFailure(requestId, "Сталася неочікувана внутрішня помилка: " + e.getMessage(), logTopic);
        } finally {
            if (tempDir != null) {
                try {
                    gitService.deleteTempDirectory(tempDir);
                    logInfo("Тимчасову директорію видалено.", logTopic);
                } catch (Exception e) {
                    logError("Не вдалося видалити тимчасову директорію: " + tempDir, logTopic);
                }
            }
        }
    }

    private void applyCachedResults(Long requestId, CachedAnalysisBundle cached, String logTopic) {
        logInfo("Знайдено кешовані результати для цього коміту. Пропускаю клонування та аналіз.", logTopic);
        List<AnalysisResult> rows = cached.getResults();
        logInfo("Збереження " + rows.size() + " результатів...", logTopic);
        transactionTemplate.executeWithoutResult(status -> {
            AnalysisRequest request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found"));
            request = entityManager.merge(request);
            if (cached.getQualityScore() != null) {
                request.setQualityScore(cached.getQualityScore());
                requestRepository.save(request);
            }
            for (AnalysisResult row : rows) {
                AnalysisResult result = new AnalysisResult();
                result.setRequest(request);
                result.setFilePath(row.getFilePath());
                result.setLineNumber(row.getLineNumber());
                result.setSeverity(row.getSeverity());
                result.setMessage(row.getMessage());
                result.setAnalyzerType(row.getAnalyzerType());
                result.setCodeSnippet(row.getCodeSnippet());
                resultRepository.save(result);
            }
            entityManager.flush();
        });
        logInfo("Результати успішно збережено в базу даних.", logTopic);
        if (cached.getQualityScore() != null) {
            logInfo("Показник якості (Quality Score): " + cached.getQualityScore() + "/100", logTopic);
        }
        updateStatusAndLog(requestId, AnalysisRequest.RequestStatus.COMPLETED,
                "Аналіз завершено (з кешу). Знайдено " + rows.size() + " порушень.", logTopic);
    }

    /**
     * Updates the analysis request status and sends a log message via WebSocket.
     */
    private void updateStatusAndLog(Long requestId, AnalysisRequest.RequestStatus status,
            String message, String topic) {
        transactionTemplate.executeWithoutResult(tx -> {
            AnalysisRequest r = requestRepository.findById(requestId).orElseThrow();
            r.setStatus(status);
            requestRepository.save(r);
        });
        logInfo(message, topic);
    }

    /**
     * Handles analysis failure by updating the request status and logging the error.
     */
    private void handleFailure(Long requestId, String errorMessage, String topic) {
        transactionTemplate.executeWithoutResult(tx ->
                requestRepository.findById(requestId).ifPresent(request -> {
                    request.setStatus(AnalysisRequest.RequestStatus.FAILED);
                    request.setErrorMessage(errorMessage);
                    requestRepository.save(request);
                }));
        logError(errorMessage, topic);
    }

    private void logInfo(String message, String topic) {
        messagingTemplate.convertAndSend(topic, new LogMessageDto("INFO", message));
        persistLogFromTopic(topic, "INFO", message);
    }

    private void logError(String message, String topic) {
        messagingTemplate.convertAndSend(topic, new LogMessageDto("ERROR", message));
        persistLogFromTopic(topic, "ERROR", message);
    }

    private void persistLogFromTopic(String topic, String level, String message) {
        try {
            String[] parts = topic.split("/");
            String last = parts[parts.length - 1];
            Long requestId = Long.parseLong(last);
            transactionTemplate.executeWithoutResult(tx -> {
                com.checkstylehub.analyzer.entity.AnalysisLog log =
                        new com.checkstylehub.analyzer.entity.AnalysisLog();
                log.setRequest(requestRepository.getReferenceById(requestId));
                log.setLevel(level);
                log.setMessage(message);
                log.setTimestamp(java.time.LocalDateTime.now());
                logRepository.save(log);
            });
        } catch (Exception ex) {
            log.trace("Could not persist analysis log for topic {}", topic, ex);
        }
    }

    /**
     * Reads ~10 lines surrounding the violation line from the given file.
     * Returns an empty string if the file cannot be read.
     *
     * @param absoluteFilePath absolute path to the Java source file
     * @param lineNumber       1-based line number of the violation
     * @return formatted code snippet with line numbers
     */
    private String extractCodeSnippet(String absoluteFilePath, int lineNumber) {
        try {
            List<String> lines = Files.readAllLines(Path.of(absoluteFilePath));
            int start = Math.max(0, lineNumber - 6);
            int end = Math.min(lines.size(), lineNumber + 5);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                String marker = (i + 1 == lineNumber) ? ">>>" : "   ";
                sb.append(String.format("%s %4d | %s%n", marker, i + 1, lines.get(i)));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Computes a relative file path from base to other, with Windows compatibility.
     * Handles edge cases like different drive letters and filesystem roots.
     *
     * @param base  the repository root path
     * @param other the file path to relativize
     * @return relative path as string with forward slashes
     */
    private String safeRelativizeToString(Path base, Path other) {
        try {
            if (base == null || other == null) {
                return other == null ? "" : other.toString().replace('\\', '/');
            }

            Path baseAbs = base.toAbsolutePath().normalize();
            Path otherAbs = other.toAbsolutePath().normalize();

            boolean differentFs = !Objects.equals(baseAbs.getFileSystem(), otherAbs.getFileSystem());
            boolean differentRoot = (baseAbs.getRoot() == null && otherAbs.getRoot() != null)
                    || (baseAbs.getRoot() != null && !baseAbs.getRoot().equals(otherAbs.getRoot()));

            String baseStr = baseAbs.toString();
            String otherStr = otherAbs.toString();
            String baseStrLc = baseStr.toLowerCase(Locale.ROOT);
            String otherStrLc = otherStr.toLowerCase(Locale.ROOT);
            if (otherStrLc.startsWith(baseStrLc)) {
                String trimmed = otherStr.substring(baseStr.length());
                if (trimmed.startsWith("\\") || trimmed.startsWith("/")) {
                    trimmed = trimmed.substring(1);
                }
                String normalized = trimmed.replace('\\', '/');
                if (!normalized.isEmpty()) {
                    return normalized;
                }
            }

            if (differentFs || differentRoot) {
                String repoRootName = baseAbs.getFileName() != null ? baseAbs.getFileName().toString() : null;
                if (repoRootName != null) {
                    int nameCount = otherAbs.getNameCount();
                    for (int i = 0; i < nameCount; i++) {
                        if (otherAbs.getName(i).toString().equalsIgnoreCase(repoRootName)) {
                            Path sub = otherAbs.subpath(i + 1, nameCount);
                            String candidate = sub.toString().replace('\\', '/');
                            if (!candidate.isEmpty()) {
                                return candidate;
                            }
                            break;
                        }
                    }
                }
                String filenameOnly = otherAbs.getFileName() != null
                        ? otherAbs.getFileName().toString()
                        : otherAbs.toString();
                return filenameOnly.replace('\\', '/');
            }

            String rel = baseAbs.relativize(otherAbs).toString().replace('\\', '/');
            if (rel.startsWith("../") || rel.startsWith("..\\") || rel.contains(":\\") || rel.contains(":/")) {
                if (otherStrLc.startsWith(baseStrLc)) {
                    String trimmed = otherStr.substring(baseStr.length());
                    if (trimmed.startsWith("\\") || trimmed.startsWith("/")) {
                        trimmed = trimmed.substring(1);
                    }
                    String normalized = trimmed.replace('\\', '/');
                    if (!normalized.isEmpty()) {
                        return normalized;
                    }
                }
                for (int i = 0; i < otherAbs.getNameCount(); i++) {
                    if (otherAbs.getName(i).toString().equalsIgnoreCase("src")) {
                        Path sub = otherAbs.subpath(i, otherAbs.getNameCount());
                        String candidate = sub.toString().replace('\\', '/');
                        if (!candidate.isEmpty()) {
                            return candidate;
                        }
                        break;
                    }
                }
                String leaf = otherAbs.getFileName() != null
                        ? otherAbs.getFileName().toString()
                        : otherAbs.toString();
                return leaf.replace('\\', '/');
            }
            return rel;
        } catch (IllegalArgumentException ex) {
            try {
                Path baseAbs = base.toAbsolutePath().normalize();
                Path otherAbs = other.toAbsolutePath().normalize();
                String baseStr = baseAbs.toString();
                String otherStr = otherAbs.toString();
                if (otherStr.toLowerCase(Locale.ROOT).startsWith(baseStr.toLowerCase(Locale.ROOT))) {
                    String trimmed = otherStr.substring(baseStr.length());
                    if (trimmed.startsWith("\\") || trimmed.startsWith("/")) {
                        trimmed = trimmed.substring(1);
                    }
                    return trimmed.replace('\\', '/');
                }
                String leaf = otherAbs.getFileName() != null
                        ? otherAbs.getFileName().toString()
                        : otherAbs.toString();
                return leaf.replace('\\', '/');
            } catch (RuntimeException e) {
                return other.toString().replace('\\', '/');
            }
        }
    }
}

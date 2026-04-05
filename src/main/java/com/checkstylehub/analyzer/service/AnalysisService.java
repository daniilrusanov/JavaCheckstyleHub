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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for orchestrating the complete code analysis workflow.
 * Handles repository cloning, Checkstyle and PMD execution, result persistence, and logging.
 * When RabbitMQ is enabled, work is consumed from a queue; otherwise the controller runs this on a task executor.
 */
@Service
public class AnalysisService {

    private final GitService gitService;
    private final CheckstyleService checkstyleService;
    private final PmdService pmdService;
    private final MetricsCalculationService metricsCalculationService;
    private final AnalysisRequestRepository requestRepository;
    private final AnalysisResultRepository resultRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.checkstylehub.analyzer.repository.AnalysisLogRepository logRepository;
    private final EntityManager entityManager;
    private final RedisTemplate<String, CachedAnalysisBundle> redisTemplate;
    private final TransactionTemplate transactionTemplate;

    public AnalysisService(GitService gitService,
                           CheckstyleService checkstyleService,
                           PmdService pmdService,
                           MetricsCalculationService metricsCalculationService,
                           AnalysisRequestRepository requestRepository,
                           AnalysisResultRepository resultRepository,
                           com.checkstylehub.analyzer.repository.AnalysisLogRepository logRepository,
                           SimpMessagingTemplate messagingTemplate,
                           EntityManager entityManager,
                           @Qualifier("analysisResultsRedisTemplate")
                           RedisTemplate<String, CachedAnalysisBundle> redisTemplate,
                           TransactionTemplate transactionTemplate) {
        this.gitService = gitService;
        this.checkstyleService = checkstyleService;
        this.pmdService = pmdService;
        this.metricsCalculationService = metricsCalculationService;
        this.requestRepository = requestRepository;
        this.resultRepository = resultRepository;
        this.logRepository = logRepository;
        this.messagingTemplate = messagingTemplate;
        this.entityManager = entityManager;
        this.redisTemplate = redisTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Executes the complete analysis workflow.
     * Steps: clone repository → find Java files → run Checkstyle → run PMD → save results.
     * Status updates and logs are sent via WebSocket in real-time.
     *
     * @param requestId              the ID of the analysis request
     * @param customCheckstyleConfig optional custom Checkstyle XML configuration
     */
    public void startAnalysisFlow(Long requestId, String customCheckstyleConfig) {
        String logTopic = "/topic/logs/" + requestId;
        Path tempDir = null;
        String cacheKey = null;
        CachedAnalysisBundle resultsForCache = null;

        try {
            String repoUrl = transactionTemplate.execute(status ->
                    requestRepository.findById(requestId)
                            .map(AnalysisRequest::getRepoUrl)
                            .orElseThrow(() -> new RuntimeException("Request not found")));

            logInfo("Перевіряю кеш та отримую останній коміт з віддаленого репозиторію...", logTopic);
            String commitHash = gitService.getLatestCommitHash(repoUrl);
            cacheKey = repoUrl + "_" + commitHash;

            CachedAnalysisBundle cached = null;
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

            updateStatusAndLog(requestId, AnalysisRequest.RequestStatus.ANALYZING, "Запуск аналізу Checkstyle...", logTopic);

            List<com.puppycrawl.tools.checkstyle.api.AuditEvent> checkstyleViolations =
                    checkstyleService.runCheckstyle(tempDir, javaFiles, customCheckstyleConfig);

            logInfo("Checkstyle завершено. Знайдено " + checkstyleViolations.size() + " порушень.", logTopic);
            logInfo("Запуск PMD (ruleset " + PmdService.DEFAULT_RULESET + ")...", logTopic);

            List<PmdService.PmdViolation> pmdViolations = pmdService.runPmd(tempDir, javaFiles);

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

            if (cacheKey != null && resultsForCache != null) {
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
    private void updateStatusAndLog(Long requestId, AnalysisRequest.RequestStatus status, String message, String topic) {
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
        } catch (Exception ignore) {
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

            boolean differentFs = baseAbs.getFileSystem() != otherAbs.getFileSystem();
            boolean differentRoot = (baseAbs.getRoot() == null && otherAbs.getRoot() != null)
                    || (baseAbs.getRoot() != null && !baseAbs.getRoot().equals(otherAbs.getRoot()));

            String baseStr = baseAbs.toString();
            String otherStr = otherAbs.toString();
            String baseStrLc = baseStr.toLowerCase();
            String otherStrLc = otherStr.toLowerCase();
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
                String filenameOnly = otherAbs.getFileName() != null ? otherAbs.getFileName().toString() : otherAbs.toString();
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
                return (otherAbs.getFileName() != null ? otherAbs.getFileName().toString() : otherAbs.toString()).replace('\\', '/');
            }
            return rel;
        } catch (IllegalArgumentException ex) {
            try {
                Path baseAbs = base.toAbsolutePath().normalize();
                Path otherAbs = other.toAbsolutePath().normalize();
                String baseStr = baseAbs.toString();
                String otherStr = otherAbs.toString();
                if (otherStr.toLowerCase().startsWith(baseStr.toLowerCase())) {
                    String trimmed = otherStr.substring(baseStr.length());
                    if (trimmed.startsWith("\\") || trimmed.startsWith("/")) {
                        trimmed = trimmed.substring(1);
                    }
                    return trimmed.replace('\\', '/');
                }
                return (otherAbs.getFileName() != null ? otherAbs.getFileName().toString() : otherAbs.toString()).replace('\\', '/');
            } catch (Exception e) {
                return other.toString().replace('\\', '/');
            }
        }
    }
}

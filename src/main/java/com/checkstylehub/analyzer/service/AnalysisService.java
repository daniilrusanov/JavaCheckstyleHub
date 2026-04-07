package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.CachedAnalysisBundle;
import com.checkstylehub.analyzer.dto.LogMessageDto;
import com.checkstylehub.analyzer.entity.AnalysisRequest;
import com.checkstylehub.analyzer.entity.AnalysisResult;
import com.checkstylehub.analyzer.entity.AnalyzerType;
import com.checkstylehub.analyzer.exception.RepositoryAccessException;
import com.checkstylehub.analyzer.repository.AnalysisRequestRepository;
import com.checkstylehub.analyzer.repository.AnalysisResultRepository;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisSystemException;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
     * Steps: clone the repository → find Java files → run Checkstyle and PMD in parallel → save results.
     * Status updates and logs are sent via WebSocket in real-time.
     *
     * @param requestId              the ID of the analysis request
     * @param customCheckstyleConfig optional custom Checkstyle XML configuration
     */
    public void startAnalysisFlow(Long requestId, String customCheckstyleConfig) {
        String logTopic = "/topic/logs/" + requestId;
        Optional<Path> tempDirHolder = Optional.empty();

        try {
            String repoUrl = transactionTemplate.execute(status ->
                    requestRepository.findById(requestId)
                            .map(AnalysisRequest::getRepoUrl)
                            .orElseThrow(() -> new IllegalStateException("Request not found")));

            logInfo("Перевіряю кеш та отримую останній коміт з віддаленого репозиторію...", logTopic);
            String commitHash = gitService.getLatestCommitHash(repoUrl);

            String effectiveConfig = (customCheckstyleConfig != null && !customCheckstyleConfig.isBlank())
                    ? customCheckstyleConfig
                    : configurationService.getActiveConfigurationXml();
            String configHash = org.springframework.util.DigestUtils.md5DigestAsHex(
                    effectiveConfig.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String cacheKey = repoUrl + "_" + commitHash + "_" + configHash;

            CachedAnalysisBundle cached = readCachedBundle(cacheKey);
            if (cached != null && cached.getResults() != null) {
                applyCachedResults(requestId, cached, logTopic);
                return;
            }

            updateStatusAndLog(requestId, AnalysisRequest.RequestStatus.CLONING, "Починаю клонування...", logTopic);
            Path tempDir = gitService.cloneRepository(repoUrl);
            tempDirHolder = Optional.of(tempDir);

            runFreshAnalysis(requestId, customCheckstyleConfig, logTopic, cacheKey, tempDir);

        } catch (RepositoryAccessException | IllegalStateException | InterruptedException | IOException |
                 DataAccessException | PersistenceException e) {
            handleFailure(requestId, e.getMessage(), logTopic);
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Analysis completion failed for request {}", requestId, e);
            handleFailure(requestId, "Сталася неочікувана внутрішня помилка: " + cause.getMessage(), logTopic);
        } finally {
            tempDirHolder.ifPresent(tempDir -> {
                gitService.deleteTempDirectory(tempDir);
                logInfo("Тимчасову директорію видалено.", logTopic);
            });
        }
    }

    private CachedAnalysisBundle readCachedBundle(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (RedisSystemException e) {
            log.trace("Redis cache read failed for key {} with error {}", cacheKey, e.getMessage());
            return null;
        }
    }

    private void runFreshAnalysis(Long requestId, String customCheckstyleConfig, String logTopic,
            String cacheKey, Path tempDir) throws IOException {

        logInfo("Клонування завершено. Шукаю Java файли...", logTopic);
        List<Path> javaFiles = checkstyleService.findJavaFiles(tempDir);
        if (javaFiles.isEmpty()) {
            throw new IllegalStateException("Репозиторій не містить файлів Java (.java). Аналіз неможливий.");
        }
        logInfo("Знайдено " + javaFiles.size() + " Java файлів. Запускаю аналіз...", logTopic);

        long linesOfCode = countLinesOfCodeOrFail(javaFiles);

        updateStatusAndLog(requestId, AnalysisRequest.RequestStatus.ANALYZING,
                "Запуск Checkstyle та PMD паралельно (ruleset " + PmdService.DEFAULT_RULESET + ")...", logTopic);

        final Path analysisRoot = tempDir;
        final List<Path> analysisJavaFiles = javaFiles;
        final String analysisCheckstyleConfig = customCheckstyleConfig;

        CompletableFuture<List<AuditEvent>> checkstyleFuture =
                CompletableFuture.supplyAsync(() -> runCheckstyleForAnalysis(
                        analysisRoot, analysisJavaFiles, analysisCheckstyleConfig, logTopic));

        CompletableFuture<List<PmdService.PmdViolation>> pmdFuture =
                CompletableFuture.supplyAsync(() -> runPmdForAnalysis(
                        analysisRoot, analysisJavaFiles, logTopic));

        CompletableFuture.allOf(checkstyleFuture, pmdFuture).join();

        List<AuditEvent> checkstyleViolations = checkstyleFuture.join();
        List<PmdService.PmdViolation> pmdViolations = pmdFuture.join();

        logInfo("Checkstyle завершено. Знайдено " + checkstyleViolations.size() + " порушень.", logTopic);
        logInfo("PMD завершено. Знайдено " + pmdViolations.size() + " порушень.", logTopic);

        int totalViolations = checkstyleViolations.size() + pmdViolations.size();
        logInfo("Збереження " + totalViolations + " результатів...", logTopic);

        final Path cloneRoot = tempDir;
        final List<AuditEvent> csEvents = checkstyleViolations;
        final List<PmdService.PmdViolation> pmdEvents = pmdViolations;
        final long loc = linesOfCode;

        CachedAnalysisBundle resultsForCache = transactionTemplate.execute(status ->
                persistAnalysisResultsAndBuildCache(requestId, cloneRoot, csEvents, pmdEvents, loc));

        int qualityScore = resultsForCache != null && resultsForCache.getQualityScore() != null
                ? resultsForCache.getQualityScore()
                : 0;
        logInfo("Результати успішно збережено в базу даних.", logTopic);
        logInfo("Показник якості (Quality Score): " + qualityScore + "/100", logTopic);

        if (resultsForCache != null) {
            writeCacheBundle(cacheKey, resultsForCache, logTopic);
        }

        updateStatusAndLog(requestId, AnalysisRequest.RequestStatus.COMPLETED,
                "Аналіз завершено. Знайдено " + totalViolations + " порушень (Checkstyle: "
                        + checkstyleViolations.size() + ", PMD: " + pmdViolations.size() + ").", logTopic);
    }

    private long countLinesOfCodeOrFail(List<Path> javaFiles) {
        try {
            return metricsCalculationService.countLinesOfCode(javaFiles);
        } catch (IOException e) {
            throw new IllegalStateException("Не вдалося підрахувати рядки коду (LOC): " + e.getMessage(), e);
        }
    }

    private List<AuditEvent> runCheckstyleForAnalysis(Path analysisRoot, List<Path> analysisJavaFiles,
            String analysisCheckstyleConfig, String logTopic) {
        try {
            return checkstyleService.runCheckstyle(
                    analysisRoot, analysisJavaFiles, analysisCheckstyleConfig);
        } catch (CheckstyleException e) {
            logInfo("Checkstyle завершився з помилкою: " + e.getMessage(), logTopic);
            return new ArrayList<>();
        }
    }

    private List<PmdService.PmdViolation> runPmdForAnalysis(Path analysisRoot, List<Path> analysisJavaFiles,
            String logTopic) {
        try {
            return pmdService.runPmd(analysisRoot, analysisJavaFiles);
        } catch (IllegalStateException e) {
            logInfo("PMD завершився з помилкою: " + e.getMessage(), logTopic);
            return new ArrayList<>();
        }
    }

    private CachedAnalysisBundle persistAnalysisResultsAndBuildCache(Long requestId, Path cloneRoot,
            List<AuditEvent> csEvents, List<PmdService.PmdViolation> pmdEvents, long loc) {
        AnalysisRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Request not found"));
        request = entityManager.merge(request);

        int qualityScore = metricsCalculationService.computeQualityScore(
                csEvents, pmdEvents, loc, false);
        request.setQualityScore(qualityScore);
        requestRepository.save(request);

        CachedAnalysisBundle bundle = new CachedAnalysisBundle();
        bundle.setLinesOfCode(loc);
        bundle.setQualityScore(qualityScore);
        List<AnalysisResult> cachedResults = new ArrayList<>();
        for (AuditEvent event : csEvents) {
            AnalysisResult result = new AnalysisResult();
            result.setRequest(request);
            result.setAnalyzerType(AnalyzerType.CHECKSTYLE);
            String relativePath = SafePathRelativizer.relativize(cloneRoot, Path.of(event.getFileName()));
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
            result.setFilePath(SafePathRelativizer.relativize(cloneRoot, Path.of(v.absoluteFilePath())));
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
    }

    private void writeCacheBundle(String cacheKey, CachedAnalysisBundle resultsForCache, String logTopic) {
        try {
            redisTemplate.opsForValue().set(cacheKey, resultsForCache, Duration.ofDays(7));
        } catch (RedisSystemException e) {
            logInfo("Кеш Redis недоступний, результати лише в БД: " + e.getMessage(), logTopic);
        }
    }

    private void applyCachedResults(Long requestId, CachedAnalysisBundle cached, String logTopic) {
        logInfo("Знайдено кешовані результати для цього коміту. Пропускаю клонування та аналіз.", logTopic);
        List<AnalysisResult> rows = cached.getResults();
        logInfo("Збереження " + rows.size() + " результатів...", logTopic);
        transactionTemplate.executeWithoutResult(status -> {
            AnalysisRequest request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalStateException("Request not found"));
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
        } catch (NumberFormatException | DataAccessException ex) {
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
        } catch (IOException e) {
            return "";
        }
    }
}

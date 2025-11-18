package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.LogMessageDto;
import com.checkstylehub.analyzer.entity.AnalysisRequest;
import com.checkstylehub.analyzer.exception.RepositoryAccessException;
import com.checkstylehub.analyzer.repository.AnalysisRequestRepository;
import com.checkstylehub.analyzer.repository.AnalysisResultRepository;
import jakarta.persistence.EntityManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

/**
 * Service responsible for orchestrating the complete code analysis workflow.
 * Handles repository cloning, Checkstyle execution, result persistence, and logging.
 * Operations are executed asynchronously to prevent blocking the main thread.
 */
@Service
public class AnalysisService {

    private final GitService gitService;
    private final CheckstyleService checkstyleService;
    private final AnalysisRequestRepository requestRepository;
    private final AnalysisResultRepository resultRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.checkstylehub.analyzer.repository.AnalysisLogRepository logRepository;
    private final EntityManager entityManager;

    public AnalysisService(GitService gitService,
                           CheckstyleService checkstyleService,
                           AnalysisRequestRepository requestRepository,
                           AnalysisResultRepository resultRepository,
                           com.checkstylehub.analyzer.repository.AnalysisLogRepository logRepository,
                           SimpMessagingTemplate messagingTemplate,
                           EntityManager entityManager) {
        this.gitService = gitService;
        this.checkstyleService = checkstyleService;
        this.requestRepository = requestRepository;
        this.resultRepository = resultRepository;
        this.logRepository = logRepository;
        this.messagingTemplate = messagingTemplate;
        this.entityManager = entityManager;
    }

    /**
     * Executes the complete analysis workflow asynchronously.
     * Steps: clone repository → find Java files → run Checkstyle → save results.
     * Status updates and logs are sent via WebSocket in real-time.
     *
     * @param requestId              the ID of the analysis request
     * @param customCheckstyleConfig optional custom Checkstyle XML configuration
     */
    @Async("taskExecutor")
    @Transactional
    public void startAnalysisFlow(Long requestId, String customCheckstyleConfig) {
        String logTopic = "/topic/logs/" + requestId;
        Path tempDir = null;

        try {
            AnalysisRequest request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            updateStatusAndLog(request, AnalysisRequest.RequestStatus.CLONING, "Починаю клонування...", logTopic);
            tempDir = gitService.cloneRepository(request.getRepoUrl());

            logInfo("Клонування завершено. Шукаю Java файли...", logTopic);
            List<Path> javaFiles = checkstyleService.findJavaFiles(tempDir);
            if (javaFiles.isEmpty()) {
                throw new IllegalStateException("Репозиторій не містить файлів Java (.java). Аналіз неможливий.");
            }
            logInfo("Знайдено " + javaFiles.size() + " Java файлів. Запускаю аналіз...", logTopic);

            updateStatusAndLog(request, AnalysisRequest.RequestStatus.ANALYZING, "Запуск аналізу Checkstyle...", logTopic);

            List<com.puppycrawl.tools.checkstyle.api.AuditEvent> violations =
                    checkstyleService.runCheckstyle(tempDir, javaFiles, customCheckstyleConfig);

            logInfo("Збереження " + violations.size() + " результатів...", logTopic);
            request = entityManager.merge(request);

            for (com.puppycrawl.tools.checkstyle.api.AuditEvent event : violations) {
                com.checkstylehub.analyzer.entity.AnalysisResult result = new com.checkstylehub.analyzer.entity.AnalysisResult();
                result.setRequest(request);
                String relativePath = safeRelativizeToString(tempDir, Path.of(event.getFileName()));
                result.setFilePath(relativePath);
                result.setLineNumber(event.getLine());
                result.setSeverity(event.getSeverityLevel().getName());
                result.setMessage(event.getMessage());
                resultRepository.save(result);
            }

            entityManager.flush();
            logInfo("Результати успішно збережено в базу даних.", logTopic);

            updateStatusAndLog(request, AnalysisRequest.RequestStatus.COMPLETED,
                    "Аналіз завершено. Знайдено " + violations.size() + " порушень.", logTopic);

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

    /**
     * Updates the analysis request status and sends a log message via WebSocket.
     */
    private void updateStatusAndLog(AnalysisRequest request, AnalysisRequest.RequestStatus status, String message, String topic) {
        request.setStatus(status);
        requestRepository.save(request);
        logInfo(message, topic);
    }

    /**
     * Handles analysis failure by updating the request status and logging the error.
     */
    private void handleFailure(Long requestId, String errorMessage, String topic) {
        requestRepository.findById(requestId).ifPresent(request -> {
            request.setStatus(AnalysisRequest.RequestStatus.FAILED);
            request.setErrorMessage(errorMessage);
            requestRepository.save(request);
        });
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
            com.checkstylehub.analyzer.entity.AnalysisLog log = new com.checkstylehub.analyzer.entity.AnalysisLog();
            log.setRequest(requestRepository.getReferenceById(requestId));
            log.setLevel(level);
            log.setMessage(message);
            log.setTimestamp(java.time.LocalDateTime.now());
            logRepository.save(log);
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

package com.checkstylehub.analyzer.controller;

import com.checkstylehub.analyzer.config.RabbitMQConfig;
import com.checkstylehub.analyzer.dto.AnalysisQueueMessage;
import com.checkstylehub.analyzer.dto.AnalysisRequestDto;
import com.checkstylehub.analyzer.dto.AnalysisRequestStatusDto;
import com.checkstylehub.analyzer.dto.AnalysisResultDto;
import com.checkstylehub.analyzer.dto.CodeAnalysisRequestDto;
import com.checkstylehub.analyzer.dto.CodeAnalysisResponseDto;
import com.checkstylehub.analyzer.entity.AnalysisRequest;
import com.checkstylehub.analyzer.entity.AnalysisResult;
import com.checkstylehub.analyzer.entity.User;
import com.checkstylehub.analyzer.repository.AnalysisRequestRepository;
import com.checkstylehub.analyzer.repository.AnalysisResultRepository;
import com.checkstylehub.analyzer.repository.UserRepository;
import com.checkstylehub.analyzer.service.AnalysisService;
import com.checkstylehub.analyzer.service.DirectCodeAnalysisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing code analysis operations.
 * Provides endpoints for starting analysis, checking status, and retrieving results.
 */
@RestController
@RequestMapping("/api")
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final AnalysisService analysisService;
    private final TaskExecutor taskExecutor;
    private final DirectCodeAnalysisService directCodeAnalysisService;
    private final AnalysisRequestRepository requestRepository;
    private final AnalysisResultRepository resultRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AnalysisController(ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
                              AnalysisService analysisService,
                              @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
                              DirectCodeAnalysisService directCodeAnalysisService,
                              AnalysisRequestRepository requestRepository,
                              AnalysisResultRepository resultRepository,
                              UserRepository userRepository,
                              ObjectMapper objectMapper) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.analysisService = analysisService;
        this.taskExecutor = taskExecutor;
        this.directCodeAnalysisService = directCodeAnalysisService;
        this.requestRepository = requestRepository;
        this.resultRepository = resultRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Initiates a new Checkstyle analysis for the specified repository.
     *
     * @param requestDto DTO containing repository URL and optional Checkstyle configuration
     * @param user the authenticated user (automatically injected by Spring Security)
     * @return ResponseEntity with the created request ID
     */
    @PostMapping("/analyze")
    @Transactional
    public ResponseEntity<Long> startAnalysis(
            @RequestBody AnalysisRequestDto requestDto,
            @AuthenticationPrincipal User user) {
        if (requestDto.getRepoUrl() == null || requestDto.getRepoUrl().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AnalysisRequest request = new AnalysisRequest(requestDto.getRepoUrl());
        if (user != null) {
            // JWT дає від’єднаний від сесії Hibernate User — беремо managed-запис з БД
            userRepository.findById(user.getId()).ifPresent(request::setUser);
        }
        AnalysisRequest savedRequest = requestRepository.save(request);
        Long id = savedRequest.getId();
        String checkstyleConfig = requestDto.getCheckstyleConfig();

        scheduleAnalysisDispatchAfterCommit(id, checkstyleConfig);

        return ResponseEntity.ok(savedRequest.getId());
    }

    /**
     * Запускає відправку в Rabbit або in-process аналіз лише після commit транзакції з {@code save(request)}.
     * Інакше фоновий потік викликає {@link AnalysisService#startAnalysisFlow} до commit і не знаходить рядок у БД.
     */
    private void scheduleAnalysisDispatchAfterCommit(Long id, String checkstyleConfig) {
        final String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(new AnalysisQueueMessage(id, checkstyleConfig));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не вдалося сформувати повідомлення для черги", e);
        }

        Runnable dispatch = () -> {
            RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
            if (rabbitTemplate != null) {
                try {
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.ANALYSIS_EXCHANGE,
                            RabbitMQConfig.ANALYSIS_ROUTING_KEY,
                            jsonPayload);
                } catch (Exception e) {
                    if (isRabbitBrokerUnreachable(e)) {
                        log.warn("RabbitMQ недоступний ({}), аналіз запускається в процесі бекенду", e.getMessage());
                    } else if (e instanceof RuntimeException re) {
                        log.error("Помилка відправки в RabbitMQ, аналіз запускається в процесі бекенду", re);
                    } else {
                        log.error("Помилка відправки в RabbitMQ, аналіз запускається в процесі бекенду", e);
                    }
                    taskExecutor.execute(() -> analysisService.startAnalysisFlow(id, checkstyleConfig));
                }
            } else {
                taskExecutor.execute(() -> analysisService.startAnalysisFlow(id, checkstyleConfig));
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }

    /**
     * Retrieves the current status of an analysis request.
     *
     * @param id the analysis request ID
     * @return ResponseEntity with the request status information
     * @throws ResponseStatusException if the request is not found
     */
    @GetMapping("/status/{id}")
    public ResponseEntity<AnalysisRequestStatusDto> getAnalysisStatus(@PathVariable Long id) {
        return requestRepository.findById(id)
                .map(req -> new AnalysisRequestStatusDto(
                        req.getId(),
                        req.getStatus() != null ? req.getStatus().name() : null,
                        req.getErrorMessage(),
                        req.getCreatedAt(),
                        req.getQualityScore()
                ))
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
    }

    /**
     * Retrieves all Checkstyle violations found during analysis.
     *
     * @param id the analysis request ID
     * @return ResponseEntity with a list of analysis results (violations)
     */
    @GetMapping("/results/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AnalysisResultDto>> getAnalysisResults(@PathVariable Long id) {
        if (!requestRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        List<AnalysisResult> results = resultRepository.findByRequestId(id);
        List<AnalysisResultDto> dtoList = results.stream()
                .map(r -> new AnalysisResultDto(
                        r.getId(),
                        r.getFilePath(),
                        r.getLineNumber(),
                        r.getSeverity(),
                        r.getMessage(),
                        r.getAnalyzerType()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    /**
     * Analyzes Java code submitted directly (without a GitHub repository).
     * Ideal for students and quick code checks.
     *
     * @param requestDto DTO containing the Java code to analyze
     * @param user the authenticated user (optional, automatically injected)
     * @return ResponseEntity with analysis results including violations and compilation status
     */
    @PostMapping("/analyze/code")
    public ResponseEntity<CodeAnalysisResponseDto> analyzeCode(
            @RequestBody CodeAnalysisRequestDto requestDto,
            @AuthenticationPrincipal User user) {
        if (requestDto.getCode() == null || requestDto.getCode().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(CodeAnalysisResponseDto.error("Код не може бути порожнім"));
        }

        CodeAnalysisResponseDto response = directCodeAnalysisService.analyzeCode(
                requestDto.getCode(),
                requestDto.getFileName(),
                requestDto.isCheckCompilation(),
                requestDto.getCheckstyleConfig()
        );

        return ResponseEntity.ok(response);
    }

    private static boolean isRabbitBrokerUnreachable(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof AmqpException) {
                return true;
            }
            if (t instanceof java.net.ConnectException) {
                return true;
            }
            String m = t.getMessage();
            if (m != null && m.contains("Connection refused")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}

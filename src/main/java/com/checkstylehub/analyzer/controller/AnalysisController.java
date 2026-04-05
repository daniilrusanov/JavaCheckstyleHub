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
import com.checkstylehub.analyzer.service.DirectCodeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
@RequiredArgsConstructor
public class AnalysisController {

    private final RabbitTemplate rabbitTemplate;
    private final DirectCodeAnalysisService directCodeAnalysisService;
    private final AnalysisRequestRepository requestRepository;
    private final AnalysisResultRepository resultRepository;
    private final UserRepository userRepository;

    /**
     * Initiates a new Checkstyle analysis for the specified repository.
     * Publishes a message to RabbitMQ; the HTTP thread returns immediately with the request id.
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
            userRepository.findById(user.getId()).ifPresent(request::setUser);
        }
        AnalysisRequest savedRequest = requestRepository.save(request);
        Long id = savedRequest.getId();
        String checkstyleConfig = requestDto.getCheckstyleConfig();

        scheduleAnalysisMessageAfterCommit(id, checkstyleConfig);

        return ResponseEntity.ok(savedRequest.getId());
    }

    /**
     * Sends the job to RabbitMQ only after the transaction that saved {@link AnalysisRequest} has committed,
     * so the consumer can load the row by id.
     */
    private void scheduleAnalysisMessageAfterCommit(Long id, String checkstyleConfig) {
        AnalysisQueueMessage message = new AnalysisQueueMessage(id, checkstyleConfig);
        Runnable send = () -> rabbitTemplate.convertAndSend(
                RabbitMQConfig.ANALYSIS_EXCHANGE,
                RabbitMQConfig.ANALYSIS_ROUTING_KEY,
                (Object) message);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
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
                .map(req -> {
                    AnalysisRequestStatusDto dto = new AnalysisRequestStatusDto();
                    dto.setId(req.getId());
                    dto.setStatus(req.getStatus() != null ? req.getStatus().name() : null);
                    dto.setErrorMessage(req.getErrorMessage());
                    dto.setCreatedAt(req.getCreatedAt());
                    dto.setQualityScore(req.getQualityScore());
                    return dto;
                })
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
                .map(r -> {
                    AnalysisResultDto dto = new AnalysisResultDto();
                    dto.setId(r.getId());
                    dto.setFilePath(r.getFilePath());
                    dto.setLineNumber(r.getLineNumber());
                    dto.setSeverity(r.getSeverity());
                    dto.setMessage(r.getMessage());
                    dto.setAnalyzerType(r.getAnalyzerType());
                    dto.setCodeSnippet(r.getCodeSnippet());
                    return dto;
                })
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
}

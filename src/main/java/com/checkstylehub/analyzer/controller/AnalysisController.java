package com.checkstylehub.analyzer.controller;

import com.checkstylehub.analyzer.dto.AnalysisRequestDto;
import com.checkstylehub.analyzer.dto.AnalysisRequestStatusDto;
import com.checkstylehub.analyzer.dto.AnalysisResultDto;
import com.checkstylehub.analyzer.entity.AnalysisRequest;
import com.checkstylehub.analyzer.entity.AnalysisResult;
import com.checkstylehub.analyzer.repository.AnalysisRequestRepository;
import com.checkstylehub.analyzer.repository.AnalysisResultRepository;
import com.checkstylehub.analyzer.service.AnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing code analysis operations.
 * Provides endpoints for starting analysis, checking status, and retrieving results.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnalysisRequestRepository requestRepository;
    private final AnalysisResultRepository resultRepository;

    public AnalysisController(AnalysisService analysisService,
                              AnalysisRequestRepository requestRepository,
                              AnalysisResultRepository resultRepository) {
        this.analysisService = analysisService;
        this.requestRepository = requestRepository;
        this.resultRepository = resultRepository;
    }

    /**
     * Initiates a new Checkstyle analysis for the specified repository.
     *
     * @param requestDto DTO containing repository URL and optional Checkstyle configuration
     * @return ResponseEntity with the created request ID
     */
    @PostMapping("/analyze")
    public ResponseEntity<Long> startAnalysis(@RequestBody AnalysisRequestDto requestDto) {
        if (requestDto.getRepoUrl() == null || requestDto.getRepoUrl().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AnalysisRequest request = new AnalysisRequest(requestDto.getRepoUrl());
        AnalysisRequest savedRequest = requestRepository.save(request);
        analysisService.startAnalysisFlow(savedRequest.getId(), requestDto.getCheckstyleConfig());

        return ResponseEntity.ok(savedRequest.getId());
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
                        req.getCreatedAt()
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
                        r.getMessage()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }
}

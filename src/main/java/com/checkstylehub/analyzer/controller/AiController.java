package com.checkstylehub.analyzer.controller;

import com.checkstylehub.analyzer.entity.AiExplanation;
import com.checkstylehub.analyzer.entity.AnalysisResult;
import com.checkstylehub.analyzer.entity.User;
import com.checkstylehub.analyzer.repository.AiExplanationRepository;
import com.checkstylehub.analyzer.repository.AnalysisResultRepository;
import com.checkstylehub.analyzer.service.AiExplanationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * REST controller for AI-powered code-violation explanations.
 * Delegates prompt construction and LLM invocation to {@link AiExplanationService}.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AnalysisResultRepository resultRepository;
    private final AiExplanationRepository explanationRepository;
    private final AiExplanationService aiExplanationService;

    public AiController(AnalysisResultRepository resultRepository,
                        AiExplanationRepository explanationRepository,
                        AiExplanationService aiExplanationService) {
        this.resultRepository = resultRepository;
        this.explanationRepository = explanationRepository;
        this.aiExplanationService = aiExplanationService;
    }

    /**
     * Generates (or returns a cached) AI explanation for a single analysis finding.
     * <p>
     * If an explanation already exists for the given result it is returned immediately
     * without calling the LLM again. Otherwise the LLM is called, the result is persisted
     * and returned as plain Markdown text.
     *
     * @param resultId the ID of the {@link AnalysisResult} to explain
     * @param user     the authenticated user (injected by Spring Security)
     * @return Markdown-formatted explanation
     */
    @PostMapping("/explain/{resultId}")
    public ResponseEntity<String> explain(
            @PathVariable Long resultId,
            @AuthenticationPrincipal User user) {

        AnalysisResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                        "Результат аналізу з id=" + resultId + " не знайдено"));

        return explanationRepository.findByAnalysisResultId(resultId)
                .map(existing -> ResponseEntity.ok(existing.getExplanation()))
                .orElseGet(() -> {
                    String markdown;
                    try {
                        markdown = aiExplanationService.explain(result, user);
                    } catch (Exception e) {
                        if (isConnectionRefused(e)) {
                            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                    "Сервіс Ollama недоступний. " +
                                    "Переконайтесь, що Ollama запущено (`ollama serve`) " +
                                    "і потрібну модель завантажено (`ollama pull llama3`).");
                        }
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Помилка генерації AI пояснення: " + e.getMessage());
                    }

                    AiExplanation entity = new AiExplanation();
                    entity.setAnalysisResult(result);
                    entity.setExplanation(markdown);
                    entity.setExperienceLevel(user.getExperienceLevel());
                    explanationRepository.save(entity);

                    return ResponseEntity.ok(markdown);
                });
    }

    /** Traverses the cause chain looking for a {@link ConnectException}. */
    private boolean isConnectionRefused(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConnectException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}

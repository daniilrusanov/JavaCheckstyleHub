package com.checkstylehub.analyzer.controller;

import com.checkstylehub.analyzer.dto.StatelessAiRequestDto;
import com.checkstylehub.analyzer.entity.AiExplanation;
import com.checkstylehub.analyzer.entity.AnalysisResult;
import com.checkstylehub.analyzer.entity.User;
import com.checkstylehub.analyzer.repository.AiExplanationRepository;
import com.checkstylehub.analyzer.repository.AnalysisRequestRepository;
import com.checkstylehub.analyzer.repository.AnalysisResultRepository;
import com.checkstylehub.analyzer.service.AiExplanationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
@RequiredArgsConstructor
public class AiController {

    private final AnalysisResultRepository resultRepository;
    private final AnalysisRequestRepository requestRepository;
    private final AiExplanationRepository explanationRepository;
    private final AiExplanationService aiExplanationService;

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

    /**
     * FRS06 — Returns a general AI-generated summary of the most frequent violations
     * found in the given analysis request, tailored to the authenticated user's
     * experience level.
     *
     * @param requestId the ID of the completed analysis request
     * @param user      the authenticated user
     * @return Markdown-formatted summary with improvement advice
     */
    @GetMapping("/summary/{requestId}")
    public ResponseEntity<String> summary(
            @PathVariable Long requestId,
            @AuthenticationPrincipal User user) {

        if (!requestRepository.existsById(requestId)) {
            throw new ResponseStatusException(NOT_FOUND,
                    "Запит на аналіз з id=" + requestId + " не знайдено");
        }

        try {
            String markdown = aiExplanationService.generateGeneralSummary(requestId, user);
            return ResponseEntity.ok(markdown);
        } catch (Exception e) {
            if (isConnectionRefused(e)) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Сервіс Ollama недоступний. " +
                        "Переконайтесь, що Ollama запущено (`ollama serve`) " +
                        "і потрібну модель завантажено (`ollama pull llama3`).");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Помилка генерації зведення: " + e.getMessage());
        }
    }

    /**
     * Stateless AI explanation for direct code analysis: extracts ~10 lines around
     * {@link StatelessAiRequestDto #lineNumber}
     * and generates Markdown without persisting an {@link AiExplanation}.
     */
    @PostMapping("/explain/stateless")
    public ResponseEntity<String> explainStateless(
            @RequestBody StatelessAiRequestDto body,
            @AuthenticationPrincipal User user) {

        String snippet = extractSnippetAroundLine(body.getCode(), body.getLineNumber());
        try {
            String markdown = aiExplanationService.generateStatelessExplanation(
                    snippet,
                    body.getMessage(),
                    user);
            return ResponseEntity.ok(markdown);
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
    }

    /**
     * Returns up to 10 lines centered on {@code lineNumber} (1-based), clamped to the source.
     */
    static String extractSnippetAroundLine(String code, int lineNumber) {
        if (code == null || code.isBlank()) {
            return "(фрагмент коду недоступний)";
        }
        String[] lines = code.split("\\R", -1);
        if (lines.length == 0) {
            return "(фрагмент коду недоступний)";
        }
        int idx = lineNumber - 1;
        if (idx < 0) {
            idx = 0;
        }
        if (idx >= lines.length) {
            idx = lines.length - 1;
        }
        int start = Math.max(0, idx - 4);
        int end = Math.min(lines.length - 1, idx + 5);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            if (i > start) {
                sb.append('\n');
            }
            sb.append(lines[i]);
        }
        return sb.toString();
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

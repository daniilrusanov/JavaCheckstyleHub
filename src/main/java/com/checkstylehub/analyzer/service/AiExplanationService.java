package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.entity.AnalysisResult;
import com.checkstylehub.analyzer.entity.ExperienceLevel;
import com.checkstylehub.analyzer.entity.User;
import com.checkstylehub.analyzer.repository.AnalysisResultRepository;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Generates AI-powered explanations for code violations using a local Ollama LLM.
 * The prompt style and detail level adapt to the user's experience level.
 */
@Service
@RequiredArgsConstructor
public class AiExplanationService {

    private static final PromptTemplate STUDENT_TEMPLATE = PromptTemplate.from("""
            Ти досвідчений Java-розробник та ментор. Поясни наступне порушення правил статичного аналізу \
            студенту-початківцю детально і зрозуміло ВИКЛЮЧНО УКРАЇНСЬКОЮ МОВОЮ.

            **Файл:** {{filePath}}
            **Рядок:** {{lineNumber}}
            **Порушення:** {{message}}

            **Фрагмент коду:**
            ```java
            {{codeSnippet}}
            ```

            Будь ласка, надай відповідь у форматі Markdown:
            1. **Що означає це правило** — теоретичне пояснення, чому воно важливе.
            2. **Що конкретно не так** — поясни помилку у наведеному коді.
            3. **Виправлений варіант** — повний виправлений приклад коду з коментарями.
            """);

    private static final PromptTemplate JUNIOR_TEMPLATE = PromptTemplate.from("""
            Ти Senior Java-розробник. Поясни порушення статичного аналізу УКРАЇНСЬКОЮ МОВОЮ для джун-розробника.

            **Файл:** {{filePath}}
            **Рядок:** {{lineNumber}}
            **Порушення:** {{message}}

            **Фрагмент коду:**
            ```java
            {{codeSnippet}}
            ```

            Надай відповідь у форматі Markdown:
            1. **Суть порушення** — коротке пояснення правила.
            2. **Виправлений код** — виправлений фрагмент без зайвих коментарів.
            """);

    private static final PromptTemplate ADVANCED_TEMPLATE = PromptTemplate.from("""
            Code review finding — respond in UKRAINIAN, keep it brief and technical.

            **File:** {{filePath}}  **Line:** {{lineNumber}}
            **Violation:** {{message}}

            ```java
            {{codeSnippet}}
            ```

            Provide:
            1. **One-line summary** of the violation (Ukrainian).
            2. **Fixed snippet** — corrected code only, no prose comments.
            """);

    private static final PromptTemplate SUMMARY_TEMPLATE = PromptTemplate.from("""
            Ти — досвідчений Java-ментор. Користувач (рівень: {{experienceLevel}}) щойно проаналізував свій код. \
            Ось його найчастіші помилки:

            {{topErrors}}

            Напиши короткий загальний висновок (до 3-4 абзаців) українською мовою з порадами щодо покращення \
            стилю написання коду. Використовуй Markdown.
            """);

    private final OllamaChatModel chatModel;
    private final AnalysisResultRepository analysisResultRepository;

    /**
     * Generates a Markdown explanation for the given violation tailored to the user's experience level.
     *
     * @param result the analysis finding containing violation details and the code snippet
     * @param user   the requesting user whose experience level determines the prompt style
     * @return Markdown-formatted AI response
     */
    public String explain(AnalysisResult result, User user) {
        ExperienceLevel level = user.getExperienceLevel() != null
                ? user.getExperienceLevel()
                : ExperienceLevel.STUDENT;

        PromptTemplate template = switch (level) {
            case STUDENT -> STUDENT_TEMPLATE;
            case JUNIOR -> JUNIOR_TEMPLATE;
            case ADVANCED -> ADVANCED_TEMPLATE;
        };

        String snippet = result.getCodeSnippet() != null && !result.getCodeSnippet().isBlank()
                ? result.getCodeSnippet()
                : "(фрагмент коду недоступний)";

        Prompt prompt = template.apply(Map.of(
                "filePath", result.getFilePath(),
                "lineNumber", String.valueOf(result.getLineNumber()),
                "message", result.getMessage(),
                "codeSnippet", snippet
        ));

        return chatModel.generate(prompt.text());
    }

    /**
     * FRS06 — Generates a general Markdown summary of the most frequent violations
     * found in a completed analysis, tailored to the user's experience level.
     *
     * @param requestId the analysis request to summarise
     * @param user      the requesting user whose experience level is included in the prompt
     * @return Markdown-formatted summary with improvement advice
     */
    public String generateGeneralSummary(Long requestId, User user) {
        List<Object[]> rows = analysisResultRepository
                .findTopErrorsByRequestId(requestId, PageRequest.of(0, 5));

        if (rows.isEmpty()) {
            return "Порушень не знайдено — код відповідає всім перевіреним правилам.";
        }

        String topErrors = IntStream.range(0, rows.size())
                .mapToObj(i -> {
                    Object[] row = rows.get(i);
                    String message = (String) row[0];
                    long count = ((Number) row[1]).longValue();
                    return (i + 1) + ". **" + message + "** (" + count + " разів)";
                })
                .collect(Collectors.joining("\n"));

        ExperienceLevel level = user.getExperienceLevel() != null
                ? user.getExperienceLevel()
                : ExperienceLevel.STUDENT;

        Prompt prompt = SUMMARY_TEMPLATE.apply(Map.of(
                "experienceLevel", level.name(),
                "topErrors", topErrors
        ));

        return chatModel.generate(prompt.text());
    }
}

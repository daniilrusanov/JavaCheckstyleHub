package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.entity.AnalysisResult;
import com.checkstylehub.analyzer.entity.ExperienceLevel;
import com.checkstylehub.analyzer.entity.User;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

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

    private final OllamaChatModel chatModel;

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
}

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

    // ==========================================
    // 1. ШАБЛОНИ ДЛЯ ЗВИЧАЙНОГО ПОЯСНЕННЯ
    // ==========================================
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
            Ти старший (Senior) Java-розробник. Поясни порушення статичного аналізу ВИКЛЮЧНО УКРАЇНСЬКОЮ МОВОЮ \
            для молодшого (Junior) розробника.

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
            Ти головний інженер (Principal Engineer). Проводиш перевірку коду. \
            Відповідай ВИКЛЮЧНО УКРАЇНСЬКОЮ МОВОЮ, коротко і технічно.

            **Файл:** {{filePath}}  **Рядок:** {{lineNumber}}
            **Порушення:** {{message}}

            ```java
            {{codeSnippet}}
            ```

            Надай:
            1. **Суть порушення** (один рядок українською).
            2. **Виправлений код** (тільки виправлений код, без пояснень).
            """);


    // ==========================================
    // 2. ШАБЛОНИ ДЛЯ ЗАГАЛЬНОГО ВИСНОВКУ (SUMMARY)
    // ==========================================
    private static final PromptTemplate SUMMARY_STUDENT_TEMPLATE = PromptTemplate.from("""
            Ти — терплячий Java-ментор. Користувач щойно проаналізував свій репозиторій.
            Ось його найчастіші помилки:
            {{topErrors}}

            Відповідай ВИКЛЮЧНО УКРАЇНСЬКОЮ МОВОЮ.
            Напиши загальний висновок щодо якості коду. Дай доброзичливі поради, як уникати цих помилок у майбутньому.
            Пояснюй простою мовою, використовуй списки для зручності читання.
            УВАГА: Не намагайся писати приклади коду, оскільки ти не бачиш вихідного коду, лише назви помилок.
            """);

    private static final PromptTemplate SUMMARY_JUNIOR_TEMPLATE = PromptTemplate.from("""
            Ти — старший (Senior) Java-розробник. Проведи загальний огляд коду на основі топ помилок розробника:
            {{topErrors}}

            УВАГА: Твоя відповідь має бути ВИКЛЮЧНО УКРАЇНСЬКОЮ МОВОЮ. Не використовуй англійську для тексту!
            Напиши конструктивний відгук форматом Markdown. Вкажи, які кращі практики порушено і як ці помилки можуть
            вплинути на підтримку проєкту в майбутньому.
            Використовуй професійну термінологію, але будь конструктивним. Не генеруй уявний код.
            """);

    private static final PromptTemplate SUMMARY_ADVANCED_TEMPLATE = PromptTemplate.from("""
            Ти — головний інженер (Principal Engineer). Це загальний звіт результатів статичного аналізу:
            {{topErrors}}

            Відповідай ВИКЛЮЧНО УКРАЇНСЬКОЮ МОВОЮ.
            Надай максимально коротке і сухе резюме (bullet points). Вкажи архітектурні чи стилістичні ризики.
            Ніяких вітань чи вступних слів. Тільки технічні факти.
            """);


    // ==========================================
    // 3. ШАБЛОНИ ДЛЯ DIRECT CODE (STATELESS)
    // ==========================================
    private static final PromptTemplate STATELESS_STUDENT_TEMPLATE = PromptTemplate.from("""
            Ти досвідчений Java-розробник та ментор. Поясни наступне порушення правил статичного аналізу \
            студенту-початківцю детально і зрозуміло ВИКЛЮЧНО УКРАЇНСЬКОЮ МОВОЮ.

            **Порушення:** {{message}}

            **Фрагмент коду:**
            ```java
            {{codeSnippet}}
            ```

            Будь ласка, надай відповідь у форматі Markdown:
            1. **Що означає це правило** — теоретичне пояснення, чому воно важливе.
            2. **Що конкретно не так** — поясни помилку у наведеному коді.
            3. **Виправлений варіант** — повний виправлений приклад коду з детальними коментарями.
            """);

    private static final PromptTemplate STATELESS_JUNIOR_TEMPLATE = PromptTemplate.from("""
            Ти старший (Senior) Java-розробник. Поясни порушення статичного аналізу ВИКЛЮЧНО УКРАЇНСЬКОЮ МОВОЮ \
            для молодшого (Junior) розробника.

            **Порушення:** {{message}}

            **Фрагмент коду:**
            ```java
            {{codeSnippet}}
            ```

            Надай відповідь у форматі Markdown:
            1. **Суть порушення** — коротке пояснення правила.
            2. **Виправлений код** — виправлений фрагмент (тільки суть, без зайвих коментарів).
            """);

    private static final PromptTemplate STATELESS_ADVANCED_TEMPLATE = PromptTemplate.from("""
            Ти головний інженер (Principal Engineer). Проводиш перевірку коду. \
            Відповідай ВИКЛЮЧНО УКРАЇНСЬКОЮ МОВОЮ, строго, коротко і технічно.

            **Порушення:** {{message}}

            ```java
            {{codeSnippet}}
            ```

            Надай:
            1. **Суть порушення** (один рядок українською).
            2. **Виправлений код** (тільки виправлений код, без пояснень).
            """);

    private final OllamaChatModel chatModel;
    private final AnalysisResultRepository analysisResultRepository;

    /**
     * Experience level for prompts and persistence when the caller is anonymous or has no level set.
     */
    public static ExperienceLevel resolveExperienceLevel(User user) {
        if (user == null || user.getExperienceLevel() == null) {
            return ExperienceLevel.STUDENT;
        }
        return user.getExperienceLevel();
    }

    /**
     * Generates a Markdown explanation for the given violation tailored to the user's experience level.
     *
     * @param result the analysis finding containing violation details and the code snippet
     * @param user   the requesting user whose experience level determines the prompt style
     * @return Markdown-formatted AI response
     */
    public String explain(AnalysisResult result, User user) {
        ExperienceLevel level = resolveExperienceLevel(user);

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
     * @param requestId the analysis request to summarize
     * @param user      the requesting user whose experience level is included in the prompt
     * @return Markdown-formatted summary with improvement advice
     */
    public String generateGeneralSummary(Long requestId, User user) {
        List<Object[]> rows = analysisResultRepository
                .findTopErrorsByRequestId(requestId, PageRequest.of(0, 10));

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

        ExperienceLevel level = resolveExperienceLevel(user);

        PromptTemplate summaryTemplate = switch (level) {
            case STUDENT -> SUMMARY_STUDENT_TEMPLATE;
            case JUNIOR -> SUMMARY_JUNIOR_TEMPLATE;
            case ADVANCED -> SUMMARY_ADVANCED_TEMPLATE;
        };

        Prompt prompt = summaryTemplate.apply(Map.of(
                "topErrors", topErrors
        ));

        return chatModel.generate(prompt.text());
    }

    /**
     * Stateless explanation for a code window and violation message (no persisted {@link AnalysisResult}).
     * Prompt style matches {@link #explain(AnalysisResult, User)} for the user's experience level.
     *
     * @param codeSnippet extracted source lines around the violation
     * @param message     violation text from the analyzer
     * @param user        requesting user (experience level selects the prompt)
     * @return Markdown-formatted AI response
     */
    public String generateStatelessExplanation(String codeSnippet, String message, User user) {
        ExperienceLevel level = resolveExperienceLevel(user);

        PromptTemplate template = switch (level) {
            case STUDENT -> STATELESS_STUDENT_TEMPLATE;
            case JUNIOR -> STATELESS_JUNIOR_TEMPLATE;
            case ADVANCED -> STATELESS_ADVANCED_TEMPLATE;
        };

        String snippet = codeSnippet != null && !codeSnippet.isBlank()
                ? codeSnippet
                : "(фрагмент коду недоступний)";

        Prompt prompt = template.apply(Map.of(
                "message", message != null ? message : "",
                "codeSnippet", snippet
        ));

        return chatModel.generate(prompt.text());
    }
}

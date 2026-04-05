package com.checkstylehub.analyzer.config;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures the local Ollama chat model used for AI-powered code-violation explanations.
 * The model runs entirely on localhost and requires no external API keys.
 */
@Configuration
public class AiConfig {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:llama3}")
    private String model;

    @Bean
    public OllamaChatModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.2)
                .timeout(Duration.ofMinutes(3))
                .build();
    }
}

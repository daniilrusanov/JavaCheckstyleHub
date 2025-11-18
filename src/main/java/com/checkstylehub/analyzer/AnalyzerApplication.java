package com.checkstylehub.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot application class for Checkstyle Analyzer.
 * Enables asynchronous processing for repository analysis tasks.
 */
@SpringBootApplication
@EnableAsync
public class AnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyzerApplication.class, args);
    }
}

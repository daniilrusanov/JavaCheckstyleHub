package com.checkstylehub.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for Checkstyle Analyzer.
 * Repository analysis tasks are processed asynchronously via RabbitMQ.
 */
@SpringBootApplication
public class AnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyzerApplication.class, args);
    }
}

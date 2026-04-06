package com.checkstylehub.analyzer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test for main Spring Boot application.
 * Verifies that the application context loads successfully.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AnalyzerApplicationTest {

    @Test
    @DisplayName("Should load application context successfully")
    void contextLoads() {
        System.out.println("Тест: завантаження контексту додатку");
        System.out.println("Контекст Spring Boot успішно завантажено");
    }
}


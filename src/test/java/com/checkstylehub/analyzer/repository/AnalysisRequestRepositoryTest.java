package com.checkstylehub.analyzer.repository;

import com.checkstylehub.analyzer.entity.AnalysisRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AnalysisRequestRepository.
 * Tests database operations for analysis requests.
 */
@DataJpaTest
class AnalysisRequestRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AnalysisRequestRepository repository;

    @Test
    @DisplayName("Should save and retrieve analysis request")
    void testSaveAndFindById() {
        System.out.println("Тест: збереження та отримання запиту на аналіз");

        AnalysisRequest request = new AnalysisRequest("https://github.com/test/repo");
        request.setStatus(AnalysisRequest.RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        AnalysisRequest savedRequest = repository.save(request);
        entityManager.flush();

        Optional<AnalysisRequest> foundRequest = repository.findById(savedRequest.getId());

        assertTrue(foundRequest.isPresent(), "Запит має бути знайдено");
        assertEquals("https://github.com/test/repo", foundRequest.get().getRepoUrl());
        assertEquals(AnalysisRequest.RequestStatus.PENDING, foundRequest.get().getStatus());

        System.out.println("Запит успішно збережено та отримано з ID: " + savedRequest.getId());
    }

    @Test
    @DisplayName("Should update request status")
    void testUpdateRequestStatus() {
        System.out.println("Тест: оновлення статусу запиту");

        AnalysisRequest request = new AnalysisRequest("https://github.com/test/repo");
        request.setStatus(AnalysisRequest.RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        AnalysisRequest savedRequest = repository.save(request);
        entityManager.flush();
        entityManager.clear();

        AnalysisRequest requestToUpdate = repository.findById(savedRequest.getId()).orElseThrow();
        requestToUpdate.setStatus(AnalysisRequest.RequestStatus.COMPLETED);

        repository.save(requestToUpdate);
        entityManager.flush();
        entityManager.clear();

        AnalysisRequest updatedRequest = repository.findById(savedRequest.getId()).orElseThrow();

        assertEquals(AnalysisRequest.RequestStatus.COMPLETED, updatedRequest.getStatus());

        System.out.println("Статус успішно оновлено: " + updatedRequest.getStatus());
    }

    @Test
    @DisplayName("Should save request with error message")
    void testSaveRequestWithError() {
        System.out.println("Тест: збереження запиту з повідомленням про помилку");

        AnalysisRequest request = new AnalysisRequest("https://github.com/invalid/repo");
        request.setStatus(AnalysisRequest.RequestStatus.FAILED);
        request.setErrorMessage("Репозиторій не знайдено");
        request.setCreatedAt(LocalDateTime.now());

        AnalysisRequest savedRequest = repository.save(request);
        entityManager.flush();

        Optional<AnalysisRequest> foundRequest = repository.findById(savedRequest.getId());

        assertTrue(foundRequest.isPresent(), "Запит має бути знайдено");
        assertEquals(AnalysisRequest.RequestStatus.FAILED, foundRequest.get().getStatus());
        assertEquals("Репозиторій не знайдено", foundRequest.get().getErrorMessage());

        System.out.println("Запит з помилкою успішно збережено");
    }

    @Test
    @DisplayName("Should return empty optional for non-existent ID")
    void testFindById_NotFound() {
        System.out.println("Тест: пошук неіснуючого запиту");

        Optional<AnalysisRequest> foundRequest = repository.findById(999L);

        assertFalse(foundRequest.isPresent(), "Запит не повинен бути знайдено");

        System.out.println("Неіснуючий запит коректно не знайдено");
    }

    @Test
    @DisplayName("Should check if request exists by ID")
    void testExistsById() {
        System.out.println("Тест: перевірка існування запиту");

        AnalysisRequest request = new AnalysisRequest("https://github.com/test/repo");
        request.setCreatedAt(LocalDateTime.now());

        AnalysisRequest savedRequest = repository.save(request);
        entityManager.flush();

        boolean exists = repository.existsById(savedRequest.getId());
        boolean notExists = repository.existsById(999L);

        assertTrue(exists, "Запит повинен існувати");
        assertFalse(notExists, "Неіснуючий запит не повинен існувати");

        System.out.println("Перевірка існування працює коректно");
    }

    @Test
    @DisplayName("Should delete analysis request")
    void testDeleteRequest() {
        System.out.println("Тест: видалення запиту на аналіз");

        AnalysisRequest request = new AnalysisRequest("https://github.com/test/repo");
        request.setCreatedAt(LocalDateTime.now());

        AnalysisRequest savedRequest = repository.save(request);
        Long savedId = savedRequest.getId();
        entityManager.flush();

        repository.deleteById(savedId);
        entityManager.flush();

        Optional<AnalysisRequest> deletedRequest = repository.findById(savedId);

        assertFalse(deletedRequest.isPresent(), "Запит повинен бути видалено");

        System.out.println("Запит успішно видалено");
    }
}


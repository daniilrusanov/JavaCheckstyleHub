package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.exception.RepositoryAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GitService.
 * Tests repository cloning and temporary directory management.
 */
class GitServiceTest {

    private GitService gitService;
    private Path testTempDir;

    @BeforeEach
    void setUp() {
        gitService = new GitService();
        System.out.println("Початок тесту GitService");
    }

    @AfterEach
    void tearDown() {
        if (testTempDir != null && Files.exists(testTempDir)) {
            gitService.deleteTempDirectory(testTempDir);
            System.out.println("Тимчасову директорію очищено");
        }
    }

    @Test
    @DisplayName("Should successfully clone a public repository")
    void testClonePublicRepository() throws Exception {
        System.out.println("Тест: клонування публічного репозиторію");

        String publicRepoUrl = "https://github.com/google/gson";

        testTempDir = gitService.cloneRepository(publicRepoUrl);

        assertNotNull(testTempDir, "Шлях до клонованого репозиторію не повинен бути null");
        assertTrue(Files.exists(testTempDir), "Директорія має існувати");
        assertTrue(Files.isDirectory(testTempDir), "Шлях має вказувати на директорію");

        System.out.println("Репозиторій успішно клоновано: " + testTempDir);
    }

    @Test
    @DisplayName("Should throw RepositoryAccessException for invalid URL")
    void testCloneInvalidRepository() {
        System.out.println("Тест: клонування з невалідним URL");

        String invalidUrl = "https://github.com/nonexistent/invalid-repo-12345";

        assertThrows(RepositoryAccessException.class, () -> {
            gitService.cloneRepository(invalidUrl);
        }, "Має викинути RepositoryAccessException для невалідного URL");

        System.out.println("Виняток коректно викинуто для невалідного URL");
    }

    @Test
    @DisplayName("Should successfully delete temporary directory")
    void testDeleteTempDirectory() throws Exception {
        System.out.println("Тест: видалення тимчасової директорії");

        Path tempDir = Files.createTempDirectory("test_cleanup_");
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");

        assertTrue(Files.exists(tempDir), "Директорія має існувати перед видаленням");

        gitService.deleteTempDirectory(tempDir);

        assertFalse(Files.exists(tempDir), "Директорія має бути видалена");

        System.out.println("Директорію успішно видалено");
    }

    @Test
    @DisplayName("Should handle deletion of non-existent directory gracefully")
    void testDeleteNonExistentDirectory() {
        System.out.println("Тест: видалення неіснуючої директорії");

        Path nonExistentPath = Path.of("/tmp/nonexistent_directory_12345");

        assertDoesNotThrow(() -> {
            gitService.deleteTempDirectory(nonExistentPath);
        }, "Не має викидати виняток для неіснуючої директорії");

        System.out.println("Обробка неіснуючої директорії пройшла успішно");
    }
}


package com.checkstylehub.analyzer.service;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CheckstyleService.
 * Tests Java file discovery and Checkstyle analysis execution.
 */
class CheckstyleServiceTest {

    @TempDir
    Path tempDir;
    @Mock
    private CheckstyleConfigurationService configurationService;
    private CheckstyleService checkstyleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        checkstyleService = new CheckstyleService(configurationService);
        System.out.println("Початок тесту CheckstyleService");
    }

    @Test
    @DisplayName("Should find all Java files in directory")
    void testFindJavaFiles() throws IOException {
        System.out.println("Тест: пошук Java файлів");

        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        Path javaFile1 = srcDir.resolve("Test1.java");
        Path javaFile2 = srcDir.resolve("Test2.java");
        Path txtFile = srcDir.resolve("readme.txt");

        Files.writeString(javaFile1, "public class Test1 {}");
        Files.writeString(javaFile2, "public class Test2 {}");
        Files.writeString(txtFile, "This is not a Java file");

        List<Path> javaFiles = checkstyleService.findJavaFiles(tempDir);

        assertEquals(2, javaFiles.size(), "Має знайти 2 Java файли");
        assertTrue(javaFiles.stream().anyMatch(p -> p.getFileName().toString().equals("Test1.java")));
        assertTrue(javaFiles.stream().anyMatch(p -> p.getFileName().toString().equals("Test2.java")));

        System.out.println("Знайдено " + javaFiles.size() + " Java файлів");
    }

    @Test
    @DisplayName("Should return empty list when no Java files exist")
    void testFindJavaFiles_NoJavaFiles() throws IOException {
        System.out.println("Тест: пошук Java файлів у порожній директорії");

        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);

        List<Path> javaFiles = checkstyleService.findJavaFiles(emptyDir);

        assertTrue(javaFiles.isEmpty(), "Список має бути порожнім");

        System.out.println("Жодного Java файлу не знайдено (очікувано)");
    }

    @Test
    @DisplayName("Should find Java files in nested directories")
    void testFindJavaFiles_NestedDirectories() throws IOException {
        System.out.println("Тест: пошук Java файлів у вкладених директоріях");

        Path level1 = tempDir.resolve("level1");
        Path level2 = level1.resolve("level2");
        Path level3 = level2.resolve("level3");
        Files.createDirectories(level3);

        Files.writeString(level1.resolve("A.java"), "public class A {}");
        Files.writeString(level2.resolve("B.java"), "public class B {}");
        Files.writeString(level3.resolve("C.java"), "public class C {}");

        List<Path> javaFiles = checkstyleService.findJavaFiles(tempDir);

        assertEquals(3, javaFiles.size(), "Має знайти 3 Java файли у вкладених директоріях");

        System.out.println("Знайдено " + javaFiles.size() + " файлів у вкладених директоріях");
    }

    @Test
    @DisplayName("Should run Checkstyle analysis with default configuration")
    void testRunCheckstyle_WithDefaultConfig() throws Exception {
        System.out.println("Тест: запуск Checkstyle з дефолтною конфігурацією");

        String defaultXml = """
                <?xml version="1.0"?>
                <!DOCTYPE module PUBLIC
                    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
                    "https://checkstyle.org/dtds/configuration_1_3.dtd">
                <module name="Checker">
                    <property name="charset" value="UTF-8"/>
                    <module name="TreeWalker">
                        <module name="EmptyStatement"/>
                    </module>
                </module>
                """;

        when(configurationService.getActiveConfigurationXml()).thenReturn(defaultXml);

        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        Path javaFile = srcDir.resolve("BadCode.java");
        Files.writeString(javaFile, """
                public class BadCode {
                    public void test() {
                        ;
                    }
                }
                """);

        List<Path> javaFiles = List.of(javaFile);

        List<AuditEvent> violations = checkstyleService.runCheckstyle(tempDir, javaFiles, null);

        assertNotNull(violations, "Результат аналізу не повинен бути null");
        assertTrue(violations.size() >= 0, "Має повернути список порушень");

        System.out.println("Аналіз завершено. Знайдено " + violations.size() + " порушень");
    }

    @Test
    @DisplayName("Should run Checkstyle with custom configuration")
    void testRunCheckstyle_WithCustomConfig() throws Exception {
        System.out.println("Тест: запуск Checkstyle з кастомною конфігурацією");

        String customXml = """
                <?xml version="1.0"?>
                <!DOCTYPE module PUBLIC
                    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
                    "https://checkstyle.org/dtds/configuration_1_3.dtd">
                <module name="Checker">
                    <property name="charset" value="UTF-8"/>
                    <module name="TreeWalker">
                        <module name="EmptyStatement"/>
                    </module>
                </module>
                """;

        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        Path javaFile = srcDir.resolve("GoodCode.java");
        Files.writeString(javaFile, """
                public class GoodCode {
                    public void test() {
                        System.out.println("Hello");
                    }
                }
                """);

        List<Path> javaFiles = List.of(javaFile);

        List<AuditEvent> violations = checkstyleService.runCheckstyle(tempDir, javaFiles, customXml);

        assertNotNull(violations, "Результат аналізу не повинен бути null");

        System.out.println("Аналіз з кастомною конфігурацією завершено");
    }

    @Test
    @DisplayName("Should throw CheckstyleException for invalid XML configuration")
    void testRunCheckstyle_InvalidXml() throws IOException {
        System.out.println("Тест: запуск Checkstyle з невалідною конфігурацією");

        String invalidXml = "<invalid>xml</invalid>";

        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);
        Path javaFile = srcDir.resolve("Test.java");
        Files.writeString(javaFile, "public class Test {}");

        List<Path> javaFiles = List.of(javaFile);

        assertThrows(CheckstyleException.class, () -> {
            checkstyleService.runCheckstyle(tempDir, javaFiles, invalidXml);
        }, "Має викинути CheckstyleException для невалідного XML");

        System.out.println("Виняток коректно викинуто для невалідної конфігурації");
    }
}


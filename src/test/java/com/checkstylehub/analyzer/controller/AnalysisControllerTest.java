package com.checkstylehub.analyzer.controller;

import com.checkstylehub.analyzer.dto.AnalysisRequestDto;
import com.checkstylehub.analyzer.dto.AnalysisRequestStatusDto;
import com.checkstylehub.analyzer.dto.AnalysisResultDto;
import com.checkstylehub.analyzer.entity.AnalysisRequest;
import com.checkstylehub.analyzer.entity.AnalysisResult;
import com.checkstylehub.analyzer.repository.AnalysisRequestRepository;
import com.checkstylehub.analyzer.repository.AnalysisResultRepository;
import com.checkstylehub.analyzer.service.AnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

/**
 * Unit tests for AnalysisController.
 * Tests REST API endpoints for analysis management.
 */
class AnalysisControllerTest {

    @Mock
    private AnalysisService analysisService;

    @Mock
    private AnalysisRequestRepository requestRepository;

    @Mock
    private AnalysisResultRepository resultRepository;

    @InjectMocks
    private AnalysisController analysisController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        System.out.println("Початок тесту AnalysisController");
    }

    @Test
    @DisplayName("Should start analysis and return request ID")
    void testStartAnalysis_Success() {
        System.out.println("Тест: запуск аналізу");

        AnalysisRequestDto requestDto = new AnalysisRequestDto();
        requestDto.setRepoUrl("https://github.com/test/repo");

        AnalysisRequest savedRequest = new AnalysisRequest("https://github.com/test/repo");
        savedRequest.setId(1L);

        when(requestRepository.save(any(AnalysisRequest.class))).thenReturn(savedRequest);
        doNothing().when(analysisService).startAnalysisFlow(anyLong(), anyString());

        ResponseEntity<Long> response = analysisController.startAnalysis(requestDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody());
        verify(requestRepository, times(1)).save(any(AnalysisRequest.class));
        verify(analysisService, times(1)).startAnalysisFlow(1L, null);

        System.out.println("Аналіз успішно запущено з ID: " + response.getBody());
    }

    @Test
    @DisplayName("Should return bad request for empty repository URL")
    void testStartAnalysis_EmptyUrl() {
        System.out.println("Тест: запуск аналізу з порожнім URL");

        AnalysisRequestDto requestDto = new AnalysisRequestDto();
        requestDto.setRepoUrl("");

        ResponseEntity<Long> response = analysisController.startAnalysis(requestDto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(requestRepository, never()).save(any());
        verify(analysisService, never()).startAnalysisFlow(anyLong(), anyString());

        System.out.println("Коректно повернуто BAD_REQUEST для порожнього URL");
    }

    @Test
    @DisplayName("Should return bad request for null repository URL")
    void testStartAnalysis_NullUrl() {
        System.out.println("Тест: запуск аналізу з null URL");

        AnalysisRequestDto requestDto = new AnalysisRequestDto();
        requestDto.setRepoUrl(null);

        ResponseEntity<Long> response = analysisController.startAnalysis(requestDto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        System.out.println("Коректно повернуто BAD_REQUEST для null URL");
    }

    @Test
    @DisplayName("Should return analysis status for existing request")
    void testGetAnalysisStatus_Success() {
        System.out.println("Тест: отримання статусу аналізу");

        Long requestId = 1L;
        AnalysisRequest request = new AnalysisRequest("https://github.com/test/repo");
        request.setId(requestId);
        request.setStatus(AnalysisRequest.RequestStatus.COMPLETED);
        request.setCreatedAt(LocalDateTime.now());

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        ResponseEntity<AnalysisRequestStatusDto> response = analysisController.getAnalysisStatus(requestId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("COMPLETED", response.getBody().getStatus());
        assertEquals(requestId, response.getBody().getId());

        System.out.println("Статус аналізу отримано: " + response.getBody().getStatus());
    }

    @Test
    @DisplayName("Should return 404 for non-existent request status")
    void testGetAnalysisStatus_NotFound() {
        System.out.println("Тест: отримання статусу неіснуючого запиту");

        Long requestId = 999L;
        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> {
            analysisController.getAnalysisStatus(requestId);
        }, "Має викинути виняток для неіснуючого запиту");

        System.out.println("Коректно викинуто виняток для неіснуючого запиту");
    }

    @Test
    @DisplayName("Should return analysis results for existing request")
    void testGetAnalysisResults_Success() {
        System.out.println("Тест: отримання результатів аналізу");

        Long requestId = 1L;

        AnalysisRequest request = new AnalysisRequest("https://github.com/test/repo");
        request.setId(requestId);

        AnalysisResult result1 = new AnalysisResult();
        result1.setId(1L);
        result1.setFilePath("src/Main.java");
        result1.setLineNumber(10);
        result1.setSeverity("warning");
        result1.setMessage("Test violation");

        when(requestRepository.existsById(requestId)).thenReturn(true);
        when(resultRepository.findByRequestId(requestId)).thenReturn(List.of(result1));

        ResponseEntity<List<AnalysisResultDto>> response = analysisController.getAnalysisResults(requestId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("src/Main.java", response.getBody().get(0).getFilePath());

        System.out.println("Результати аналізу отримано: " + response.getBody().size() + " порушень");
    }

    @Test
    @DisplayName("Should return 404 for non-existent request results")
    void testGetAnalysisResults_NotFound() {
        System.out.println("Тест: отримання результатів неіснуючого запиту");

        Long requestId = 999L;
        when(requestRepository.existsById(requestId)).thenReturn(false);

        ResponseEntity<List<AnalysisResultDto>> response = analysisController.getAnalysisResults(requestId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        System.out.println("Коректно повернуто NOT_FOUND для неіснуючого запиту");
    }

    @Test
    @DisplayName("Should return empty list for request with no violations")
    void testGetAnalysisResults_NoViolations() {
        System.out.println("Тест: отримання результатів аналізу без порушень");

        Long requestId = 1L;
        when(requestRepository.existsById(requestId)).thenReturn(true);
        when(resultRepository.findByRequestId(requestId)).thenReturn(List.of());

        ResponseEntity<List<AnalysisResultDto>> response = analysisController.getAnalysisResults(requestId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        System.out.println("Результати аналізу: порушень не знайдено");
    }

    @Test
    @DisplayName("Should start analysis with custom Checkstyle configuration")
    void testStartAnalysis_WithCustomConfig() {
        System.out.println("Тест: запуск аналізу з кастомною конфігурацією");

        AnalysisRequestDto requestDto = new AnalysisRequestDto();
        requestDto.setRepoUrl("https://github.com/test/repo");
        requestDto.setCheckstyleConfig("<module name=\"Checker\"></module>");

        AnalysisRequest savedRequest = new AnalysisRequest("https://github.com/test/repo");
        savedRequest.setId(2L);

        when(requestRepository.save(any(AnalysisRequest.class))).thenReturn(savedRequest);
        doNothing().when(analysisService).startAnalysisFlow(anyLong(), anyString());

        ResponseEntity<Long> response = analysisController.startAnalysis(requestDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2L, response.getBody());
        verify(analysisService, times(1)).startAnalysisFlow(eq(2L), eq("<module name=\"Checker\"></module>"));

        System.out.println("Аналіз з кастомною конфігурацією успішно запущено");
    }
}


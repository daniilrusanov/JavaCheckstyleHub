package com.checkstylehub.analyzer.controller;

import com.checkstylehub.analyzer.dto.AnalysisRequestStatusDto;
import com.checkstylehub.analyzer.dto.UserDto;
import com.checkstylehub.analyzer.dto.UserSettingsDto;
import com.checkstylehub.analyzer.entity.AnalysisRequest;
import com.checkstylehub.analyzer.entity.ExperienceLevel;
import com.checkstylehub.analyzer.entity.Role;
import com.checkstylehub.analyzer.entity.User;
import com.checkstylehub.analyzer.entity.UserSettings;
import com.checkstylehub.analyzer.repository.AnalysisRequestRepository;
import com.checkstylehub.analyzer.repository.AnalysisResultRepository;
import com.checkstylehub.analyzer.repository.UserRepository;
import com.checkstylehub.analyzer.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserController.
 * Tests user profile, settings, history, and statistics endpoints.
 */
class UserControllerTest {

    @Mock
    private AnalysisRequestRepository analysisRequestRepository;

    @Mock
    private AnalysisResultRepository analysisResultRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController userController;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testUser = new User("testuser", "test@example.com", "encodedpassword");
        testUser.setId(1L);
        testUser.setRole(Role.USER);
        testUser.setExperienceLevel(ExperienceLevel.JUNIOR);
        
        System.out.println("Початок тесту UserController");
    }

    @Test
    @DisplayName("Should return user profile when authenticated")
    void testGetProfile_Success() {
        System.out.println("Тест: отримання профілю користувача");

        ResponseEntity<UserDto> response = userController.getProfile(testUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals("test@example.com", response.getBody().getEmail());
        assertEquals("JUNIOR", response.getBody().getExperienceLevel());

        System.out.println("Профіль користувача: " + response.getBody().getUsername());
    }

    @Test
    @DisplayName("Should return 401 when not authenticated for profile")
    void testGetProfile_NotAuthenticated() {
        System.out.println("Тест: отримання профілю без автентифікації");

        ResponseEntity<UserDto> response = userController.getProfile(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("Коректно повернуто 401");
    }

    @Test
    @DisplayName("Should update experience level successfully")
    void testUpdateExperienceLevel_Success() {
        System.out.println("Тест: оновлення рівня досвіду");

        Map<String, String> request = new HashMap<>();
        request.put("experienceLevel", "ADVANCED");

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userSettingsRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        ResponseEntity<UserDto> response = userController.updateExperienceLevel(testUser, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userRepository, times(1)).save(any(User.class));

        System.out.println("Рівень досвіду оновлено");
    }

    @Test
    @DisplayName("Should return bad request for invalid experience level")
    void testUpdateExperienceLevel_Invalid() {
        System.out.println("Тест: оновлення з невалідним рівнем досвіду");

        Map<String, String> request = new HashMap<>();
        request.put("experienceLevel", "INVALID_LEVEL");

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        ResponseEntity<UserDto> response = userController.updateExperienceLevel(testUser, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        System.out.println("Коректно повернуто BAD_REQUEST для невалідного рівня");
    }

    @Test
    @DisplayName("Should return bad request when experience level is missing")
    void testUpdateExperienceLevel_MissingField() {
        System.out.println("Тест: оновлення без поля experienceLevel");

        Map<String, String> request = new HashMap<>();

        ResponseEntity<UserDto> response = userController.updateExperienceLevel(testUser, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        System.out.println("Коректно повернуто BAD_REQUEST для відсутнього поля");
    }

    @Test
    @DisplayName("Should return 401 when not authenticated for experience level update")
    void testUpdateExperienceLevel_NotAuthenticated() {
        System.out.println("Тест: оновлення рівня без автентифікації");

        Map<String, String> request = new HashMap<>();
        request.put("experienceLevel", "ADVANCED");

        ResponseEntity<UserDto> response = userController.updateExperienceLevel(null, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        System.out.println("Коректно повернуто 401");
    }

    @Test
    @DisplayName("Should return user settings when authenticated")
    void testGetSettings_Success() {
        System.out.println("Тест: отримання налаштувань користувача");

        UserSettings settings = new UserSettings(testUser);
        settings.setLineLength(120);
        settings.setAvoidStarImport(true);

        when(userSettingsRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(settings));

        ResponseEntity<UserSettingsDto> response = userController.getSettings(testUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(120, response.getBody().getLineLength());
        assertTrue(response.getBody().getAvoidStarImport());

        System.out.println("Налаштування отримано, lineLength: " + response.getBody().getLineLength());
    }

    @Test
    @DisplayName("Should create default settings if none exist")
    void testGetSettings_CreateDefault() {
        System.out.println("Тест: створення налаштувань за замовчуванням");

        UserSettings defaultSettings = new UserSettings(testUser);

        when(userSettingsRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class))).thenReturn(defaultSettings);

        ResponseEntity<UserSettingsDto> response = userController.getSettings(testUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userSettingsRepository, times(1)).save(any(UserSettings.class));

        System.out.println("Налаштування за замовчуванням створено");
    }

    @Test
    @DisplayName("Should return 401 when not authenticated for settings")
    void testGetSettings_NotAuthenticated() {
        System.out.println("Тест: отримання налаштувань без автентифікації");

        ResponseEntity<UserSettingsDto> response = userController.getSettings(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        System.out.println("Коректно повернуто 401");
    }

    @Test
    @DisplayName("Should update user settings successfully")
    void testUpdateSettings_Success() {
        System.out.println("Тест: оновлення налаштувань користувача");

        UserSettings existingSettings = new UserSettings(testUser);
        UserSettingsDto settingsDto = new UserSettingsDto();
        settingsDto.setLineLength(150);
        settingsDto.setAvoidStarImport(true);

        when(userSettingsRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(existingSettings));
        when(userSettingsRepository.save(any(UserSettings.class))).thenReturn(existingSettings);

        ResponseEntity<UserSettingsDto> response = userController.updateSettings(testUser, settingsDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userSettingsRepository, times(1)).save(any(UserSettings.class));

        System.out.println("Налаштування оновлено");
    }

    @Test
    @DisplayName("Should return 401 when not authenticated for settings update")
    void testUpdateSettings_NotAuthenticated() {
        System.out.println("Тест: оновлення налаштувань без автентифікації");

        UserSettingsDto settingsDto = new UserSettingsDto();

        ResponseEntity<UserSettingsDto> response = userController.updateSettings(null, settingsDto);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        System.out.println("Коректно повернуто 401");
    }

    @Test
    @DisplayName("Should return analysis history when authenticated")
    void testGetAnalysisHistory_Success() {
        System.out.println("Тест: отримання історії аналізів");

        AnalysisRequest request1 = new AnalysisRequest("https://github.com/user/repo1");
        request1.setId(1L);
        request1.setStatus(AnalysisRequest.RequestStatus.COMPLETED);
        request1.setCreatedAt(LocalDateTime.now());
        request1.setUser(testUser);

        AnalysisRequest request2 = new AnalysisRequest("https://github.com/user/repo2");
        request2.setId(2L);
        request2.setStatus(AnalysisRequest.RequestStatus.FAILED);
        request2.setErrorMessage("Repository not found");
        request2.setCreatedAt(LocalDateTime.now().minusDays(1));
        request2.setUser(testUser);

        when(analysisRequestRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
                .thenReturn(List.of(request1, request2));
        when(analysisResultRepository.countByRequestId(1L)).thenReturn(5L);

        ResponseEntity<List<AnalysisRequestStatusDto>> response = userController.getAnalysisHistory(testUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("COMPLETED", response.getBody().get(0).getStatus());
        assertEquals(5L, response.getBody().get(0).getViolationsCount());
        assertEquals("FAILED", response.getBody().get(1).getStatus());
        assertNull(response.getBody().get(1).getViolationsCount());

        System.out.println("Історія отримана, кількість записів: " + response.getBody().size());
    }

    @Test
    @DisplayName("Should return empty history for new user")
    void testGetAnalysisHistory_Empty() {
        System.out.println("Тест: порожня історія для нового користувача");

        when(analysisRequestRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
                .thenReturn(List.of());

        ResponseEntity<List<AnalysisRequestStatusDto>> response = userController.getAnalysisHistory(testUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        System.out.println("Історія порожня");
    }

    @Test
    @DisplayName("Should return 401 when not authenticated for history")
    void testGetAnalysisHistory_NotAuthenticated() {
        System.out.println("Тест: отримання історії без автентифікації");

        ResponseEntity<List<AnalysisRequestStatusDto>> response = userController.getAnalysisHistory(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        System.out.println("Коректно повернуто 401");
    }

    @Test
    @DisplayName("Should return user statistics when authenticated")
    void testGetStatistics_Success() {
        System.out.println("Тест: отримання статистики користувача");

        AnalysisRequest request1 = new AnalysisRequest("https://github.com/user/repo1");
        request1.setId(1L);
        request1.setStatus(AnalysisRequest.RequestStatus.COMPLETED);

        AnalysisRequest request2 = new AnalysisRequest("https://github.com/user/repo2");
        request2.setId(2L);
        request2.setStatus(AnalysisRequest.RequestStatus.COMPLETED);

        AnalysisRequest request3 = new AnalysisRequest("https://github.com/user/repo1");
        request3.setId(3L);
        request3.setStatus(AnalysisRequest.RequestStatus.FAILED);

        when(analysisRequestRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
                .thenReturn(List.of(request1, request2, request3));
        when(analysisResultRepository.countByRequestId(1L)).thenReturn(10L);
        when(analysisResultRepository.countByRequestId(2L)).thenReturn(5L);

        ResponseEntity<Map<String, Object>> response = userController.getStatistics(testUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().get("totalAnalyses"));
        assertEquals(2L, response.getBody().get("completedAnalyses"));
        assertEquals(1L, response.getBody().get("failedAnalyses"));
        assertEquals(15L, response.getBody().get("totalViolations"));
        assertEquals(2L, response.getBody().get("uniqueRepositories"));

        System.out.println("Статистика: загалом аналізів - " + response.getBody().get("totalAnalyses"));
    }

    @Test
    @DisplayName("Should return zero statistics for new user")
    void testGetStatistics_Empty() {
        System.out.println("Тест: статистика для нового користувача");

        when(analysisRequestRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = userController.getStatistics(testUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().get("totalAnalyses"));
        assertEquals(0L, response.getBody().get("completedAnalyses"));
        assertEquals(0L, response.getBody().get("failedAnalyses"));
        assertEquals(0L, response.getBody().get("totalViolations"));
        assertEquals(0L, response.getBody().get("uniqueRepositories"));

        System.out.println("Статистика порожня для нового користувача");
    }

    @Test
    @DisplayName("Should return 401 when not authenticated for statistics")
    void testGetStatistics_NotAuthenticated() {
        System.out.println("Тест: отримання статистики без автентифікації");

        ResponseEntity<Map<String, Object>> response = userController.getStatistics(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        System.out.println("Коректно повернуто 401");
    }
}

package com.checkstylehub.analyzer.controller;

import com.checkstylehub.analyzer.dto.AuthResponseDto;
import com.checkstylehub.analyzer.dto.LoginRequestDto;
import com.checkstylehub.analyzer.dto.RegisterRequestDto;
import com.checkstylehub.analyzer.dto.UserDto;
import com.checkstylehub.analyzer.entity.ExperienceLevel;
import com.checkstylehub.analyzer.entity.Role;
import com.checkstylehub.analyzer.entity.User;
import com.checkstylehub.analyzer.exception.AuthenticationException;
import com.checkstylehub.analyzer.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthController.
 * Tests authentication REST API endpoints.
 */
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        System.out.println("Початок тесту AuthController");
    }

    @Test
    @DisplayName("Should register user successfully")
    void testRegister_Success() {
        System.out.println("Тест: успішна реєстрація користувача");

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AuthResponseDto expectedResponse = new AuthResponseDto(
                "jwt-token-123",
                "testuser",
                "test@example.com",
                "USER",
                "STUDENT"
        );

        when(authService.register(any(RegisterRequestDto.class))).thenReturn(expectedResponse);

        ResponseEntity<AuthResponseDto> response = authController.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals("test@example.com", response.getBody().getEmail());
        assertEquals("jwt-token-123", response.getBody().getToken());
        verify(authService, times(1)).register(any(RegisterRequestDto.class));

        System.out.println("Користувача успішно зареєстровано: " + response.getBody().getUsername());
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void testRegister_UsernameExists() {
        System.out.println("Тест: реєстрація з існуючим ім'ям користувача");

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(authService.register(any(RegisterRequestDto.class)))
                .thenThrow(new AuthenticationException("Користувач з таким ім'ям вже існує"));

        assertThrows(AuthenticationException.class, () -> {
            authController.register(request);
        });

        System.out.println("Коректно викинуто виняток для існуючого користувача");
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegister_EmailExists() {
        System.out.println("Тест: реєстрація з існуючим email");

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(authService.register(any(RegisterRequestDto.class)))
                .thenThrow(new AuthenticationException("Користувач з таким email вже існує"));

        assertThrows(AuthenticationException.class, () -> {
            authController.register(request);
        });

        System.out.println("Коректно викинуто виняток для існуючого email");
    }

    @Test
    @DisplayName("Should login user successfully")
    void testLogin_Success() {
        System.out.println("Тест: успішний вхід користувача");

        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("testuser");
        request.setPassword("password123");

        AuthResponseDto expectedResponse = new AuthResponseDto(
                "jwt-token-456",
                "testuser",
                "test@example.com",
                "USER",
                "JUNIOR"
        );

        when(authService.login(any(LoginRequestDto.class))).thenReturn(expectedResponse);

        ResponseEntity<AuthResponseDto> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals("jwt-token-456", response.getBody().getToken());
        verify(authService, times(1)).login(any(LoginRequestDto.class));

        System.out.println("Користувач успішно увійшов: " + response.getBody().getUsername());
    }

    @Test
    @DisplayName("Should throw exception for invalid credentials")
    void testLogin_InvalidCredentials() {
        System.out.println("Тест: вхід з невірними даними");

        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new AuthenticationException("Невірне ім'я користувача або пароль"));

        assertThrows(AuthenticationException.class, () -> {
            authController.login(request);
        });

        System.out.println("Коректно викинуто виняток для невірних даних");
    }

    @Test
    @DisplayName("Should return current user when authenticated")
    void testGetCurrentUser_Authenticated() {
        System.out.println("Тест: отримання поточного користувача");

        User user = new User("testuser", "test@example.com", "encodedpassword");
        user.setId(1L);
        user.setRole(Role.USER);
        user.setExperienceLevel(ExperienceLevel.JUNIOR);

        ResponseEntity<UserDto> response = authController.getCurrentUser(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals("test@example.com", response.getBody().getEmail());

        System.out.println("Поточний користувач: " + response.getBody().getUsername());
    }

    @Test
    @DisplayName("Should return 401 when user is not authenticated")
    void testGetCurrentUser_NotAuthenticated() {
        System.out.println("Тест: отримання користувача без автентифікації");

        ResponseEntity<UserDto> response = authController.getCurrentUser(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("Коректно повернуто 401 для неавторизованого запиту");
    }

    @Test
    @DisplayName("Should return user with correct role")
    void testGetCurrentUser_WithAdminRole() {
        System.out.println("Тест: отримання користувача з роллю ADMIN");

        User adminUser = new User("admin", "admin@example.com", "encodedpassword");
        adminUser.setId(1L);
        adminUser.setRole(Role.ADMIN);
        adminUser.setExperienceLevel(ExperienceLevel.ADVANCED);

        ResponseEntity<UserDto> response = authController.getCurrentUser(adminUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ADMIN", response.getBody().getRole());

        System.out.println("Роль користувача: " + response.getBody().getRole());
    }

    @Test
    @DisplayName("Should register user with default experience level")
    void testRegister_DefaultExperienceLevel() {
        System.out.println("Тест: реєстрація з рівнем досвіду за замовчуванням");

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");

        AuthResponseDto expectedResponse = new AuthResponseDto(
                "jwt-token-789",
                "newuser",
                "newuser@example.com",
                "USER",
                "STUDENT"
        );

        when(authService.register(any(RegisterRequestDto.class))).thenReturn(expectedResponse);

        ResponseEntity<AuthResponseDto> response = authController.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("STUDENT", response.getBody().getExperienceLevel());

        System.out.println("Рівень досвіду за замовчуванням: " + response.getBody().getExperienceLevel());
    }
}

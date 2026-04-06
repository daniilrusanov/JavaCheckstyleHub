package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.AuthResponseDto;
import com.checkstylehub.analyzer.dto.LoginRequestDto;
import com.checkstylehub.analyzer.dto.RegisterRequestDto;
import com.checkstylehub.analyzer.entity.ExperienceLevel;
import com.checkstylehub.analyzer.entity.Role;
import com.checkstylehub.analyzer.entity.User;
import com.checkstylehub.analyzer.exception.AuthenticationException;
import com.checkstylehub.analyzer.repository.UserRepository;
import com.checkstylehub.analyzer.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService.
 * Tests authentication and registration business logic.
 */
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        System.out.println("Початок тесту AuthService");
    }

    @Test
    @DisplayName("Should register new user successfully")
    void testRegister_Success() {
        System.out.println("Тест: успішна реєстрація нового користувача");

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token-123");

        AuthResponseDto response = authService.register(request);

        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        assertEquals("newuser@example.com", response.getEmail());
        assertEquals("jwt-token-123", response.getToken());
        assertEquals("USER", response.getRole());
        assertEquals("STUDENT", response.getExperienceLevel());

        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
        verify(jwtService, times(1)).generateToken(any(User.class));

        System.out.println("Користувача успішно зареєстровано: " + response.getUsername());
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void testRegister_UsernameExists() {
        System.out.println("Тест: реєстрація з існуючим username");

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authService.register(request);
        });

        assertEquals("Користувач з таким ім'ям вже існує", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));

        System.out.println("Коректно викинуто виняток для існуючого username");
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegister_EmailExists() {
        System.out.println("Тест: реєстрація з існуючим email");

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authService.register(request);
        });

        assertEquals("Користувач з таким email вже існує", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));

        System.out.println("Коректно викинуто виняток для існуючого email");
    }

    @Test
    @DisplayName("Should login user successfully")
    void testLogin_Success() {
        System.out.println("Тест: успішний вхід користувача");

        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("testuser");
        request.setPassword("password123");

        User user = new User("testuser", "test@example.com", "encodedPassword");
        user.setId(1L);
        user.setRole(Role.USER);
        user.setExperienceLevel(ExperienceLevel.JUNIOR);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("testuser", "password123"));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token-456");

        AuthResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("jwt-token-456", response.getToken());
        assertEquals("USER", response.getRole());
        assertEquals("JUNIOR", response.getExperienceLevel());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).save(user); // Updates lastLoginAt

        System.out.println("Користувач успішно увійшов: " + response.getUsername());
    }

    @Test
    @DisplayName("Should throw exception for invalid credentials")
    void testLogin_InvalidCredentials() {
        System.out.println("Тест: вхід з невірними даними");

        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authService.login(request);
        });

        assertEquals("Невірне ім'я користувача або пароль", exception.getMessage());
        verify(userRepository, never()).findByUsername(anyString());

        System.out.println("Коректно викинуто виняток для невірних даних");
    }

    @Test
    @DisplayName("Should throw exception when user not found after authentication")
    void testLogin_UserNotFound() {
        System.out.println("Тест: користувача не знайдено після автентифікації");

        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("nonexistent", "password123"));
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authService.login(request);
        });

        assertEquals("Користувача не знайдено", exception.getMessage());

        System.out.println("Коректно викинуто виняток для неіснуючого користувача");
    }

    @Test
    @DisplayName("Should encode password during registration")
    void testRegister_PasswordEncoding() {
        System.out.println("Тест: шифрування паролю при реєстрації");

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("plainPassword");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("token");

        authService.register(request);

        verify(passwordEncoder, times(1)).encode("plainPassword");

        System.out.println("Пароль успішно зашифровано");
    }

    @Test
    @DisplayName("Should set default role as USER during registration")
    void testRegister_DefaultRole() {
        System.out.println("Тест: роль за замовчуванням при реєстрації");

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(Role.USER, savedUser.getRole());
            return savedUser;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("token");

        AuthResponseDto response = authService.register(request);

        assertEquals("USER", response.getRole());

        System.out.println("Роль за замовчуванням: USER");
    }

    @Test
    @DisplayName("Should update lastLoginAt on successful login")
    void testLogin_UpdatesLastLoginAt() {
        System.out.println("Тест: оновлення lastLoginAt при вході");

        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("testuser");
        request.setPassword("password123");

        User user = new User("testuser", "test@example.com", "encodedPassword");
        user.setId(1L);
        user.setRole(Role.USER);
        user.setExperienceLevel(ExperienceLevel.STUDENT);

        assertNull(user.getLastLoginAt());

        when(authenticationManager.authenticate(any())).thenReturn(mock(UsernamePasswordAuthenticationToken.class));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertNotNull(savedUser.getLastLoginAt());
            return savedUser;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("token");

        authService.login(request);

        verify(userRepository, times(1)).save(any(User.class));

        System.out.println("lastLoginAt оновлено при вході");
    }

    @Test
    @DisplayName("Should generate JWT token during registration")
    void testRegister_GeneratesToken() {
        System.out.println("Тест: генерація JWT при реєстрації");

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("generated-jwt-token");

        AuthResponseDto response = authService.register(request);

        assertEquals("generated-jwt-token", response.getToken());
        verify(jwtService, times(1)).generateToken(any(User.class));

        System.out.println("JWT токен згенеровано: " + response.getToken());
    }
}

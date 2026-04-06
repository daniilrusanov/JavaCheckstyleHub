package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.entity.ExperienceLevel;
import com.checkstylehub.analyzer.entity.Role;
import com.checkstylehub.analyzer.entity.User;
import com.checkstylehub.analyzer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserService.
 * Tests user management operations and Spring Security integration.
 */
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testUser = new User("testuser", "test@example.com", "encodedPassword");
        testUser.setId(1L);
        testUser.setRole(Role.USER);
        testUser.setExperienceLevel(ExperienceLevel.JUNIOR);
        
        System.out.println("Початок тесту UserService");
    }

    @Test
    @DisplayName("Should load user by username for Spring Security")
    void testLoadUserByUsername_Success() {
        System.out.println("Тест: завантаження користувача за username");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        verify(userRepository, times(1)).findByUsername("testuser");

        System.out.println("Користувача знайдено: " + userDetails.getUsername());
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user not found")
    void testLoadUserByUsername_NotFound() {
        System.out.println("Тест: користувача не знайдено");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("nonexistent");
        });

        assertTrue(exception.getMessage().contains("nonexistent"));

        System.out.println("Коректно викинуто виняток: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should find user by username")
    void testFindByUsername_Success() {
        System.out.println("Тест: пошук користувача за username");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        User user = userService.findByUsername("testuser");

        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());

        System.out.println("Користувача знайдено: " + user.getUsername());
    }

    @Test
    @DisplayName("Should throw exception when finding non-existent user by username")
    void testFindByUsername_NotFound() {
        System.out.println("Тест: пошук неіснуючого користувача за username");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.findByUsername("nonexistent");
        });

        assertTrue(exception.getMessage().contains("nonexistent"));

        System.out.println("Коректно викинуто виняток");
    }

    @Test
    @DisplayName("Should find user by ID")
    void testFindById_Success() {
        System.out.println("Тест: пошук користувача за ID");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User user = userService.findById(1L);

        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());

        System.out.println("Користувача знайдено за ID: " + user.getId());
    }

    @Test
    @DisplayName("Should throw exception when finding non-existent user by ID")
    void testFindById_NotFound() {
        System.out.println("Тест: пошук неіснуючого користувача за ID");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.findById(999L);
        });

        assertTrue(exception.getMessage().contains("999"));

        System.out.println("Коректно викинуто виняток");
    }

    @Test
    @DisplayName("Should check if username exists")
    void testExistsByUsername_True() {
        System.out.println("Тест: перевірка існування username");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        boolean exists = userService.existsByUsername("existinguser");

        assertTrue(exists);
        verify(userRepository, times(1)).existsByUsername("existinguser");

        System.out.println("Username існує: " + exists);
    }

    @Test
    @DisplayName("Should return false when username does not exist")
    void testExistsByUsername_False() {
        System.out.println("Тест: перевірка неіснуючого username");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        boolean exists = userService.existsByUsername("newuser");

        assertFalse(exists);

        System.out.println("Username не існує: " + exists);
    }

    @Test
    @DisplayName("Should check if email exists")
    void testExistsByEmail_True() {
        System.out.println("Тест: перевірка існування email");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        boolean exists = userService.existsByEmail("existing@example.com");

        assertTrue(exists);
        verify(userRepository, times(1)).existsByEmail("existing@example.com");

        System.out.println("Email існує: " + exists);
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void testExistsByEmail_False() {
        System.out.println("Тест: перевірка неіснуючого email");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        boolean exists = userService.existsByEmail("new@example.com");

        assertFalse(exists);

        System.out.println("Email не існує: " + exists);
    }

    @Test
    @DisplayName("Should save user successfully")
    void testSave_Success() {
        System.out.println("Тест: збереження користувача");

        User newUser = new User("newuser", "new@example.com", "password");
        newUser.setRole(Role.USER);
        newUser.setExperienceLevel(ExperienceLevel.STUDENT);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(2L);
            return savedUser;
        });

        User savedUser = userService.save(newUser);

        assertNotNull(savedUser);
        assertEquals(2L, savedUser.getId());
        assertEquals("newuser", savedUser.getUsername());
        verify(userRepository, times(1)).save(newUser);

        System.out.println("Користувача збережено з ID: " + savedUser.getId());
    }

    @Test
    @DisplayName("Should return UserDetails with correct authorities")
    void testLoadUserByUsername_Authorities() {
        System.out.println("Тест: перевірка authorities користувача");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userService.loadUserByUsername("testuser");

        assertNotNull(userDetails.getAuthorities());
        assertFalse(userDetails.getAuthorities().isEmpty());

        System.out.println("Authorities: " + userDetails.getAuthorities());
    }

    @Test
    @DisplayName("Should handle admin user correctly")
    void testLoadUserByUsername_Admin() {
        System.out.println("Тест: завантаження адміністратора");

        User adminUser = new User("admin", "admin@example.com", "encodedPassword");
        adminUser.setId(1L);
        adminUser.setRole(Role.ADMIN);
        adminUser.setExperienceLevel(ExperienceLevel.ADVANCED);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        UserDetails userDetails = userService.loadUserByUsername("admin");

        assertNotNull(userDetails);
        assertEquals("admin", userDetails.getUsername());

        System.out.println("Адміністратора знайдено: " + userDetails.getUsername());
    }

    @Test
    @DisplayName("Should update user correctly")
    void testSave_Update() {
        System.out.println("Тест: оновлення користувача");

        testUser.setExperienceLevel(ExperienceLevel.ADVANCED);

        when(userRepository.save(testUser)).thenReturn(testUser);

        User updatedUser = userService.save(testUser);

        assertEquals(ExperienceLevel.ADVANCED, updatedUser.getExperienceLevel());
        verify(userRepository, times(1)).save(testUser);

        System.out.println("Користувача оновлено, рівень досвіду: " + updatedUser.getExperienceLevel());
    }
}

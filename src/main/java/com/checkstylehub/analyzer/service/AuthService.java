package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.dto.AuthResponseDto;
import com.checkstylehub.analyzer.dto.LoginRequestDto;
import com.checkstylehub.analyzer.dto.RegisterRequestDto;
import com.checkstylehub.analyzer.entity.Role;
import com.checkstylehub.analyzer.entity.User;
import com.checkstylehub.analyzer.exception.AuthenticationException;
import com.checkstylehub.analyzer.repository.UserRepository;
import com.checkstylehub.analyzer.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for authentication operations.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user.
     */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        // Check if a username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthenticationException("Користувач з таким ім'ям вже існує");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthenticationException("Користувач з таким email вже існує");
        }

        // Create a new user
        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );
        user.setRole(Role.USER);

        userRepository.save(user);

        // Generate token
        String token = jwtService.generateToken(user);

        return new AuthResponseDto(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getExperienceLevel().name()
        );
    }

    /**
     * Authenticate a user and return a JWT token.
     */
    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new AuthenticationException("Невірне ім'я користувача або пароль");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthenticationException("Користувача не знайдено"));

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate token
        String token = jwtService.generateToken(user);

        return new AuthResponseDto(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getExperienceLevel().name()
        );
    }
}

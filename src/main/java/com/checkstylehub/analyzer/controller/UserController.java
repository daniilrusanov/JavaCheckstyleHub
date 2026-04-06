package com.checkstylehub.analyzer.controller;

import com.checkstylehub.analyzer.dto.AnalysisRequestStatusDto;
import com.checkstylehub.analyzer.dto.UserDto;
import com.checkstylehub.analyzer.dto.UserSettingsDto;
import com.checkstylehub.analyzer.entity.AnalysisRequest;
import com.checkstylehub.analyzer.entity.ExperienceLevel;
import com.checkstylehub.analyzer.entity.User;
import com.checkstylehub.analyzer.entity.UserSettings;
import com.checkstylehub.analyzer.repository.AnalysisRequestRepository;
import com.checkstylehub.analyzer.repository.AnalysisResultRepository;
import com.checkstylehub.analyzer.repository.UserRepository;
import com.checkstylehub.analyzer.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for user-specific endpoints.
 * Provides access to user profile, analysis history, settings, and statistics.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AnalysisRequestRepository analysisRequestRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;

    /**
     * Get current user profile.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfile(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new UserDto(user));
    }

    /**
     * Update user experience level.
     */
    @PatchMapping("/profile/experience")
    @Transactional
    public ResponseEntity<UserDto> updateExperienceLevel(
            @AuthenticationPrincipal User authUser,
            @RequestBody Map<String, String> request) {
        if (authUser == null) {
            return ResponseEntity.status(401).build();
        }

        String levelStr = request.get("experienceLevel");
        if (levelStr == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Reload user from database to ensure it's managed
            User user = userRepository.findById(authUser.getId())
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).build();
            }

            ExperienceLevel level = ExperienceLevel.valueOf(levelStr.toUpperCase(Locale.ROOT));
            user.setExperienceLevel(level);
            userRepository.save(user);

            // Also update settings timestamp since experience level is part of user settings
            userSettingsRepository.findByUserId(user.getId())
                    .ifPresent(settings -> {
                        settings.setUpdatedAt(java.time.LocalDateTime.now());
                        userSettingsRepository.save(settings);
                    });

            return ResponseEntity.ok(new UserDto(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user's Checkstyle settings.
     */
    @GetMapping("/settings")
    @Transactional
    public ResponseEntity<UserSettingsDto> getSettings(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        User managed = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalid"));

        UserSettings settings = userSettingsRepository.findByUserId(managed.getId())
                .orElseGet(() -> {
                    try {
                        return userSettingsRepository.save(new UserSettings(managed));
                    } catch (DataIntegrityViolationException e) {
                        return userSettingsRepository.findByUserId(managed.getId())
                                .orElseThrow(() -> new IllegalStateException(
                                        "Could not create or load user settings", e));
                    }
                });

        return ResponseEntity.ok(new UserSettingsDto(settings));
    }

    /**
     * Update user's Checkstyle settings.
     */
    @PutMapping("/settings")
    @Transactional
    public ResponseEntity<UserSettingsDto> updateSettings(
            @AuthenticationPrincipal User user,
            @RequestBody UserSettingsDto settingsDto) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        User managed = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalid"));

        UserSettings settings = userSettingsRepository.findByUserId(managed.getId())
                .orElseGet(() -> {
                    try {
                        return userSettingsRepository.save(new UserSettings(managed));
                    } catch (DataIntegrityViolationException e) {
                        return userSettingsRepository.findByUserId(managed.getId())
                                .orElseThrow(() -> new IllegalStateException(
                                        "Could not create or load user settings", e));
                    }
                });

        settingsDto.applyTo(settings);
        settings = userSettingsRepository.save(settings);

        return ResponseEntity.ok(new UserSettingsDto(settings));
    }

    /**
     * Get user's analysis history.
     */
    @GetMapping("/history")
    public ResponseEntity<List<AnalysisRequestStatusDto>> getAnalysisHistory(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<AnalysisRequest> requests =
                analysisRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<AnalysisRequestStatusDto> history = requests.stream()
                .map(req -> {
                    Long violationsCount =
                            req.getStatus() == AnalysisRequest.RequestStatus.COMPLETED
                            ? analysisResultRepository.countByRequestId(req.getId())
                            : null;
                    return new AnalysisRequestStatusDto(
                            req.getId(),
                            req.getStatus() != null ? req.getStatus().name() : null,
                            req.getErrorMessage(),
                            req.getCreatedAt(),
                            req.getRepoUrl(),
                            violationsCount,
                            req.getQualityScore()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    /**
     * Get user's analysis statistics.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<AnalysisRequest> requests =
                analysisRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAnalyses", requests.size());
        stats.put("completedAnalyses", requests.stream()
                .filter(r -> r.getStatus() == AnalysisRequest.RequestStatus.COMPLETED)
                .count());
        stats.put("failedAnalyses", requests.stream()
                .filter(r -> r.getStatus() == AnalysisRequest.RequestStatus.FAILED)
                .count());

        // Count total violations across all completed analyses
        long totalViolations = requests.stream()
                .filter(r -> r.getStatus() == AnalysisRequest.RequestStatus.COMPLETED)
                .mapToLong(r -> analysisResultRepository.countByRequestId(r.getId()))
                .sum();
        stats.put("totalViolations", totalViolations);

        // Get unique repositories analyzed
        long uniqueRepos = requests.stream()
                .map(AnalysisRequest::getRepoUrl)
                .distinct()
                .count();
        stats.put("uniqueRepositories", uniqueRepos);

        return ResponseEntity.ok(stats);
    }
}

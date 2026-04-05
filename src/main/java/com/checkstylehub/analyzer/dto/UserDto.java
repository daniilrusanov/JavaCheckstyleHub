package com.checkstylehub.analyzer.dto;

import com.checkstylehub.analyzer.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user information (without sensitive data).
 */
@Data
@NoArgsConstructor
public class UserDto {

    private Long id;
    private String username;
    private String email;
    private String role;
    private String experienceLevel;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private int analysisCount;

    public UserDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.experienceLevel = user.getExperienceLevel().name();
        this.createdAt = user.getCreatedAt();
        this.lastLoginAt = user.getLastLoginAt();
        this.analysisCount = user.getAnalysisRequests() != null ? user.getAnalysisRequests().size() : 0;
    }
}

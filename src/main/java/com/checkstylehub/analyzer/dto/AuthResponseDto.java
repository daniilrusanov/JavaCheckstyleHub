package com.checkstylehub.analyzer.dto;

/**
 * DTO for authentication response containing JWT token and user info.
 */
public class AuthResponseDto {

    private String token;
    private String username;
    private String email;
    private String role;
    private String experienceLevel;

    public AuthResponseDto() {
    }

    public AuthResponseDto(String token, String username, String email, String role, String experienceLevel) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.role = role;
        this.experienceLevel = experienceLevel;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }
}

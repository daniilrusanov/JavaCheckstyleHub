package com.checkstylehub.analyzer.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for user login request.
 */
public class LoginRequestDto {

    @NotBlank(message = "Ім'я користувача обов'язкове")
    private String username;

    @NotBlank(message = "Пароль обов'язковий")
    private String password;

    public LoginRequestDto() {
    }

    public LoginRequestDto(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

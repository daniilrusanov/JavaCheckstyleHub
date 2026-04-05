package com.checkstylehub.analyzer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for user login request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "Ім'я користувача обов'язкове")
    private String username;

    @NotBlank(message = "Пароль обов'язковий")
    private String password;
}

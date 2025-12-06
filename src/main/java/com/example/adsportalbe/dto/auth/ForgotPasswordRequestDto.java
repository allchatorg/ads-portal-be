package com.example.adsportalbe.dto.auth;

import jakarta.validation.constraints.Size;

public record ForgotPasswordRequestDto(
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String username,
        @Size(max = 254, message = "Email must be less than 254 characters") String email) {
}

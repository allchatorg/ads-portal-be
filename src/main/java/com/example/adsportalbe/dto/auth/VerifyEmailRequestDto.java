package com.example.adsportalbe.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequestDto(
        @NotBlank(message = "Token is required") String token) {
}

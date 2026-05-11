package com.example.adsportalbe.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendPhoneVerificationRequestDto(
        @NotBlank(message = "Phone number is required")
        @Size(max = 32, message = "Phone number must be less than 32 characters")
        String phoneNumber) {
}

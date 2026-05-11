package com.example.adsportalbe.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PhonePasswordResetVerificationRequestDto(
        @NotBlank(message = "Phone number is required")
        @Size(max = 32, message = "Phone number must be less than 32 characters")
        String phoneNumber,

        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
        String verificationCode) {
}

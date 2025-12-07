package com.example.adsportalbe.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestEmailUpdateDto(
        @NotBlank(message = "Current password is required") String currentPassword,

        @NotBlank(message = "New email is required") @Email(message = "Invalid email format") String newEmail) {
}

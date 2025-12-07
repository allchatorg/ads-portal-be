package com.example.adsportalbe.dto.auth;

import com.example.adsportalbe.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequestDto(
        @NotBlank(message = "Current password is required") String currentPassword,

        @NotBlank(message = "New password is required") @ValidPassword String newPassword,

        @NotBlank(message = "Password confirmation is required") String confirmPassword) {
}

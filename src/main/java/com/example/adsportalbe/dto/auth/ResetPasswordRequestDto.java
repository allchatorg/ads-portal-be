package com.example.adsportalbe.dto.auth;

import com.example.adsportalbe.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequestDto(
        @NotBlank(message = "Token is required") String token,

        @NotBlank(message = "New password is required") @ValidPassword String newPassword) {
}

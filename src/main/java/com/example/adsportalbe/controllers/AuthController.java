package com.example.adsportalbe.controllers;

import com.example.adsportalbe.dto.UserDto;
import com.example.adsportalbe.dto.auth.*;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.services.AuthService;
import com.example.adsportalbe.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request,
                                                    HttpServletRequest httpRequest) {
        AuthResponseDto response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {
        AuthResponseDto response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        UserDto userDto = userService.getCurrentUser(user);
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.initiatePasswordReset(request);
        return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }

    @PostMapping("/send-verification")
    public ResponseEntity<Map<String, String>> sendVerificationEmail(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        userService.sendEmailVerification(user);
        return ResponseEntity.ok(Map.of("message", "Verification email sent"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequestDto request) {
        userService.verifyEmail(request.token());
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        userService.changePassword(request, user);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/request-email-update")
    public ResponseEntity<Map<String, String>> requestEmailUpdate(
            @Valid @RequestBody RequestEmailUpdateDto request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        userService.requestEmailUpdate(request, user);
        return ResponseEntity.ok(Map.of("message", "Verification code sent to new email"));
    }

    @PostMapping("/verify-email-update")
    public ResponseEntity<Map<String, String>> verifyEmailUpdate(
            @Valid @RequestBody VerifyEmailUpdateDto request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        User updatedUser = userService.verifyEmailUpdate(request, user);
        return ResponseEntity.ok(Map.of(
                "message", "Email updated successfully",
                "newEmail", updatedUser.getEmail()));
    }
}

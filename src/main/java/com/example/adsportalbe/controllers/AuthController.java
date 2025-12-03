package com.example.adsportalbe.controllers;

import com.example.adsportalbe.dto.auth.AuthResponseDto;
import com.example.adsportalbe.dto.auth.LoginRequestDto;
import com.example.adsportalbe.dto.auth.RegisterRequestDto;
import com.example.adsportalbe.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
}

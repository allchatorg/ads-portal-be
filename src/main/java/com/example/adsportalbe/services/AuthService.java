package com.example.adsportalbe.services;

import com.example.adsportalbe.dto.auth.*;
import com.example.adsportalbe.models.identity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto request, HttpServletRequest httpRequest);

    AuthResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest);

    void logout(HttpServletRequest request, HttpServletResponse response);

    void initiatePasswordReset(ForgotPasswordRequestDto request);

    PhonePasswordResetVerificationResponseDto verifyPhonePasswordReset(PhonePasswordResetVerificationRequestDto request);

    User resetPassword(ResetPasswordRequestDto request);
}

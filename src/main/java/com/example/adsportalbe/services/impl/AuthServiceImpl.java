package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.dto.auth.*;
import com.example.adsportalbe.enums.TokenType;
import com.example.adsportalbe.enums.Role;
import com.example.adsportalbe.exceptions.ConflictException;
import com.example.adsportalbe.models.UserActionToken;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.repositories.UserRepository;
import com.example.adsportalbe.services.AuthService;
import com.example.adsportalbe.services.MailService;
import com.example.adsportalbe.services.SmsSenderService;
import com.example.adsportalbe.services.UserActionTokenService;
import com.example.adsportalbe.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.util.StringUtils.hasText;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_PHONE_RESET_CODE_MESSAGE = "Invalid or expired reset code";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final com.example.adsportalbe.security.JwtUtils jwtUtils;
    private final UserActionTokenService tokenService;
    private final MailService mailService;
    private final SmsSenderService smsSenderService;
    private final UserService userService;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request, HttpServletRequest httpRequest) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Create new user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .emailVerified(false) // User needs to verify email
                .isOver18(request.isOver18())
                .acceptsPolicies(request.isAcceptsPolicies())
                .build();

        user = userRepository.save(user);

        // Generate JWT token
        String jwtToken = jwtUtils.generateToken(user);

        return AuthResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(jwtToken)
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        // Get authenticated user
        User user = (User) authentication.getPrincipal();

        // Generate JWT token
        String jwtToken = jwtUtils.generateToken(user);

        return AuthResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(jwtToken)
                .build();
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // Client should handle logout by removing the token
        SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional
    public void initiatePasswordReset(ForgotPasswordRequestDto request) {
        boolean hasEmail = hasText(request.email());
        boolean hasPhoneNumber = hasText(request.phoneNumber());
        int providedRecoveryMethods = (hasEmail ? 1 : 0) + (hasPhoneNumber ? 1 : 0);

        if (providedRecoveryMethods != 1) {
            throw new IllegalArgumentException("Provide exactly one recovery method");
        }

        if (hasPhoneNumber) {
            initiatePhonePasswordReset(request.phoneNumber());
            return;
        }

        initiateEmailPasswordReset(request.email());
    }

    private void initiateEmailPasswordReset(String email) {
        userService.findOptionalByEmail(email.trim())
                .ifPresent(user -> {
                    UserActionToken token = tokenService.createPasswordResetTokenForUser(user);

                    try {
                        mailService.sendResetPasswordEmail(user, token.getToken());
                    } catch (RuntimeException e) {
                        log.warn("Failed to send password reset email for user id={}", user.getId(), e);
                    }
                });
    }

    private void initiatePhonePasswordReset(String phoneNumber) {
        String normalizedPhoneNumber = smsSenderService.normalizePhoneNumber(phoneNumber);
        userService.findOptionalByPhoneNumber(normalizedPhoneNumber)
                .filter(this::hasVerifiedPasswordResetPhone)
                .ifPresent(user -> {
                    UserActionToken token = tokenService.createPhonePasswordResetTokenForUser(user, normalizedPhoneNumber);
                    String smsContent = "Your allChat ads portal password reset code is: " + token.getToken();
                    smsSenderService.sendSMS(normalizedPhoneNumber, smsContent);
                });
    }

    @Override
    @Transactional
    public PhonePasswordResetVerificationResponseDto verifyPhonePasswordReset(
            PhonePasswordResetVerificationRequestDto request) {
        String normalizedPhoneNumber = smsSenderService.normalizePhoneNumber(request.phoneNumber());
        UserActionToken userActionToken = findPhonePasswordResetToken(request.verificationCode().trim());

        if (userActionToken.getExpiryDate().isBefore(Instant.now())) {
            throw new ConflictException(INVALID_PHONE_RESET_CODE_MESSAGE);
        }

        if (!userActionToken.getType().equals(TokenType.PHONE_PASSWORD_RESET)) {
            throw new ConflictException(INVALID_PHONE_RESET_CODE_MESSAGE);
        }

        if (userActionToken.isUsed()) {
            throw new ConflictException(INVALID_PHONE_RESET_CODE_MESSAGE);
        }

        if (!normalizedPhoneNumber.equals(userActionToken.getPhoneNumber())) {
            throw new ConflictException(INVALID_PHONE_RESET_CODE_MESSAGE);
        }

        User user = userActionToken.getUser();
        if (!hasVerifiedPasswordResetPhone(user) || !normalizedPhoneNumber.equals(user.getPhoneNumber())) {
            throw new ConflictException(INVALID_PHONE_RESET_CODE_MESSAGE);
        }

        userActionToken.setUsed(true);
        tokenService.save(userActionToken);

        UserActionToken resetToken = tokenService.createPasswordResetTokenForUser(user);
        return new PhonePasswordResetVerificationResponseDto(resetToken.getToken());
    }

    private UserActionToken findPhonePasswordResetToken(String verificationCode) {
        try {
            return tokenService.findToken(verificationCode);
        } catch (ConflictException e) {
            throw new ConflictException(INVALID_PHONE_RESET_CODE_MESSAGE);
        }
    }

    private boolean hasVerifiedPasswordResetPhone(User user) {
        return user.getPhoneNumberVerificationDate() != null;
    }

    @Override
    @Transactional
    public User resetPassword(ResetPasswordRequestDto request) {
        return userService.resetPassword(request);
    }
}

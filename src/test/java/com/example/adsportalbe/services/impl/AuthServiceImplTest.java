package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.dto.auth.ForgotPasswordRequestDto;
import com.example.adsportalbe.dto.auth.PhonePasswordResetVerificationRequestDto;
import com.example.adsportalbe.enums.Role;
import com.example.adsportalbe.enums.TokenType;
import com.example.adsportalbe.exceptions.ConflictException;
import com.example.adsportalbe.models.UserActionToken;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.repositories.UserRepository;
import com.example.adsportalbe.security.JwtUtils;
import com.example.adsportalbe.services.MailService;
import com.example.adsportalbe.services.SmsSenderService;
import com.example.adsportalbe.services.UserActionTokenService;
import com.example.adsportalbe.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserActionTokenService tokenService;
    @Mock
    private MailService mailService;
    @Mock
    private SmsSenderService smsSenderService;
    @Mock
    private UserService userService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtUtils,
                tokenService,
                mailService,
                smsSenderService,
                userService);
    }

    @Test
    void initiatePasswordResetRequiresExactlyOneRecoveryMethod() {
        assertThatThrownBy(() -> authService.initiatePasswordReset(new ForgotPasswordRequestDto(null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Provide exactly one recovery method");

        assertThatThrownBy(() -> authService.initiatePasswordReset(new ForgotPasswordRequestDto("user@example.com", "+15555550123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Provide exactly one recovery method");
    }

    @Test
    void initiatePasswordResetDoesNotRevealUnknownEmail() {
        when(userService.findOptionalByEmail("missing@example.com")).thenReturn(Optional.empty());

        authService.initiatePasswordReset(new ForgotPasswordRequestDto("missing@example.com", null));

        verify(tokenService, never()).createPasswordResetTokenForUser(any());
        verify(mailService, never()).sendResetPasswordEmail(any(), any());
    }

    @Test
    void initiatePasswordResetDoesNotRevealUnknownPhone() {
        when(smsSenderService.normalizePhoneNumber("+1 555 555 0123")).thenReturn("+15555550123");
        when(userService.findOptionalByPhoneNumber("+15555550123")).thenReturn(Optional.empty());

        authService.initiatePasswordReset(new ForgotPasswordRequestDto(null, "+1 555 555 0123"));

        verify(tokenService, never()).createPhonePasswordResetTokenForUser(any(), any());
        verify(smsSenderService, never()).sendSMS(any(), any());
    }

    @Test
    void initiatePasswordResetSendsCodeToVerifiedPhone() {
        User user = user(1L, "user@example.com", "+15555550123", Instant.now());
        UserActionToken token = token(user, "123456", TokenType.PHONE_PASSWORD_RESET, "+15555550123");

        when(smsSenderService.normalizePhoneNumber("+1 555 555 0123")).thenReturn("+15555550123");
        when(userService.findOptionalByPhoneNumber("+15555550123")).thenReturn(Optional.of(user));
        when(tokenService.createPhonePasswordResetTokenForUser(user, "+15555550123")).thenReturn(token);

        authService.initiatePasswordReset(new ForgotPasswordRequestDto(null, "+1 555 555 0123"));

        verify(smsSenderService).sendSMS("+15555550123", "Your allChat ads portal password reset code is: ");
    }

    @Test
    void verifyPhonePasswordResetCreatesResetToken() {
        User user = user(1L, "user@example.com", "+15555550123", Instant.now());
        UserActionToken phoneToken = token(user, "123456", TokenType.PHONE_PASSWORD_RESET, "+15555550123");
        UserActionToken resetToken = token(user, "reset-token", TokenType.PASSWORD_RESET, null);

        when(smsSenderService.normalizePhoneNumber("+1 555 555 0123")).thenReturn("+15555550123");
        when(tokenService.findToken("123456")).thenReturn(phoneToken);
        when(tokenService.createPasswordResetTokenForUser(user)).thenReturn(resetToken);

        var response = authService.verifyPhonePasswordReset(
                new PhonePasswordResetVerificationRequestDto("+1 555 555 0123", "123456"));

        assertThat(response.resetToken()).isEqualTo("reset-token");
        assertThat(phoneToken.isUsed()).isTrue();
        verify(tokenService).save(phoneToken);
    }

    @Test
    void verifyPhonePasswordResetRejectsInvalidCode() {
        when(smsSenderService.normalizePhoneNumber("+1 555 555 0123")).thenReturn("+15555550123");
        when(tokenService.findToken("123456")).thenThrow(new ConflictException("Incorrect verification code: 123456"));

        assertThatThrownBy(() -> authService.verifyPhonePasswordReset(
                new PhonePasswordResetVerificationRequestDto("+1 555 555 0123", "123456")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Invalid or expired reset code");
    }

    private static User user(Long id, String email, String phoneNumber, Instant phoneVerifiedAt) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("encoded");
        user.setRole(Role.USER);
        user.setPhoneNumber(phoneNumber);
        user.setPhoneNumberVerificationDate(phoneVerifiedAt);
        return user;
    }

    private static UserActionToken token(User user, String token, TokenType type, String phoneNumber) {
        return UserActionToken.builder()
                .user(user)
                .email(user.getEmail())
                .phoneNumber(phoneNumber)
                .token(token)
                .expiryDate(Instant.now().plusSeconds(300))
                .type(type)
                .build();
    }
}

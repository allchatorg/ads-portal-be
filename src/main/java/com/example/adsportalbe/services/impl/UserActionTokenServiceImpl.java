package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.enums.TokenType;
import com.example.adsportalbe.exceptions.ConflictException;
import com.example.adsportalbe.models.UserActionToken;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.repositories.UserActionTokenRepository;
import com.example.adsportalbe.services.UserActionTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserActionTokenServiceImpl implements UserActionTokenService {

    private static final long PASSWORD_RESET_TOKEN_EXPIRATION = 60L * 60L; // 60 minutes in seconds
    private static final long EMAIL_VERIFICATION_TOKEN_EXPIRATION = 60L * 15L; // 15 minutes in seconds
    private static final long PHONE_VERIFICATION_TOKEN_EXPIRATION = 60L * 5L; // 5 minutes in seconds
    private final UserActionTokenRepository userActionTokenRepository;

    @Override
    public UserActionToken createPasswordResetTokenForUser(User user) {
        UserActionToken token = UserActionToken.builder()
                .user(user)
                .email(user.getEmail())
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusSeconds(PASSWORD_RESET_TOKEN_EXPIRATION))
                .type(TokenType.PASSWORD_RESET)
                .build();
        return save(token);
    }

    @Override
    public UserActionToken createEmailVerificationTokenForUser(User user) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty for email verification token.");
        }
        if (user.isEmailVerified()) {
            throw new IllegalStateException("User is already verified.");
        }

        String sixDigitToken = generateSixDigitToken();

        UserActionToken token = UserActionToken.builder()
                .user(user)
                .email(user.getEmail())
                .token(sixDigitToken)
                .expiryDate(Instant.now().plusSeconds(EMAIL_VERIFICATION_TOKEN_EXPIRATION))
                .type(TokenType.EMAIL_VERIFICATION)
                .build();

        return save(token);
    }

    @Override
    public UserActionToken findToken(String token) {
        return userActionTokenRepository.findByToken(token).orElseThrow(
                () -> new ConflictException("Incorrect verification code: " + token));
    }

    @Override
    public UserActionToken save(UserActionToken userActionToken) {
        return userActionTokenRepository.save(userActionToken);
    }

    @Override
    public UserActionToken createPhoneVerificationToken(User currentUser, String number) {
        String sixDigitToken = generateSixDigitToken();

        UserActionToken token = UserActionToken.builder()
                .user(currentUser)
                .email(currentUser.getEmail())
                .token(sixDigitToken)
                .expiryDate(Instant.now().plusSeconds(PHONE_VERIFICATION_TOKEN_EXPIRATION))
                .type(TokenType.PHONE_VERIFICATION)
                .phoneNumber(number)
                .build();

        return save(token);
    }

    private String generateSixDigitToken() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}

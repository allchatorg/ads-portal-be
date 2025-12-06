package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.dto.auth.ResetPasswordRequestDto;
import com.example.adsportalbe.enums.TokenType;
import com.example.adsportalbe.models.UserActionToken;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.repositories.UserRepository;
import com.example.adsportalbe.services.MailService;
import com.example.adsportalbe.services.UserActionTokenService;
import com.example.adsportalbe.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserActionTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User resetPassword(ResetPasswordRequestDto request) {
        UserActionToken token = tokenService.findToken(request.token());

        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token has expired");
        }

        if (token.isUsed()) {
            throw new IllegalArgumentException("Token has already been used");
        }

        User user = findByEmail(token.getEmail());
        user.setPassword(passwordEncoder.encode(request.newPassword()));

        token.setUsed(true);
        tokenService.save(token);

        return save(user);
    }

    @Override
    public void sendEmailVerification(User user) {
        UserActionToken token = tokenService.createEmailVerificationTokenForUser(user);
        mailService.sendVerificationEmail(user, token.getToken());
    }

    @Override
    public User verifyEmail(String token) {
        UserActionToken userActionToken = tokenService.findToken(token);

        if (userActionToken.getExpiryDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token has expired");
        }

        if (!userActionToken.getType().equals(TokenType.EMAIL_VERIFICATION)) {
            throw new IllegalArgumentException("Token is not an email verification token");
        }

        if (userActionToken.isUsed()) {
            throw new IllegalArgumentException("Token has already been used");
        }

        User user = userActionToken.getUser();
        user.setVerified(true);
        userActionToken.setUsed(true);
        tokenService.save(userActionToken);

        return save(user);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }
}

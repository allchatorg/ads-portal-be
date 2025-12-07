package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.dto.UserDto;
import com.example.adsportalbe.dto.auth.ChangePasswordRequestDto;
import com.example.adsportalbe.dto.auth.RequestEmailUpdateDto;
import com.example.adsportalbe.dto.auth.ResetPasswordRequestDto;
import com.example.adsportalbe.dto.auth.VerifyEmailUpdateDto;
import com.example.adsportalbe.enums.TokenType;
import com.example.adsportalbe.mappers.UserMapper;
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
    private final UserMapper userMapper;

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
        user.setEmailVerified(true);
        userActionToken.setUsed(true);
        tokenService.save(userActionToken);

        return save(user);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public void changePassword(ChangePasswordRequestDto request, User user) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalStateException("Wrong password");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        save(user);
    }

    @Override
    public UserDto getCurrentUser(User user) {
        return userMapper.toUserDto(user);
    }

    @Override
    public void requestEmailUpdate(RequestEmailUpdateDto request, User user) {
        // Validate current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalStateException("Wrong password");
        }

        // Check if new email is already in use
        if (existsByEmail(request.newEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        // Create email update token with new email
        UserActionToken token = tokenService.createEmailUpdateTokenForUser(user, request.newEmail());

        // Send verification code to new email address
        mailService.sendEmailUpdateVerification(request.newEmail(), token.getToken());
    }

    @Override
    public User verifyEmailUpdate(VerifyEmailUpdateDto request, User user) {
        UserActionToken token = tokenService.findToken(request.verificationCode());

        // Validate token type
        if (!token.getType().equals(TokenType.EMAIL_UPDATE)) {
            throw new IllegalArgumentException("Token is not an email update token");
        }

        // Check token not expired
        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token has expired");
        }

        // Check token not already used
        if (token.isUsed()) {
            throw new IllegalArgumentException("Token has already been used");
        }

        // Verify token belongs to authenticated user
        if (!token.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Token does not belong to the authenticated user");
        }

        // Update user's email to the new email from token
        user.setEmail(token.getEmail());
        user.setEmailVerified(false); // Require re-verification of new email

        // Mark token as used
        token.setUsed(true);
        tokenService.save(token);

        return save(user);
    }
}

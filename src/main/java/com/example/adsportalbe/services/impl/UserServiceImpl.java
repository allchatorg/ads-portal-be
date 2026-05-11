package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.dto.UserDto;
import com.example.adsportalbe.dto.auth.*;
import com.example.adsportalbe.enums.TokenType;
import com.example.adsportalbe.exceptions.ConflictException;
import com.example.adsportalbe.mappers.UserMapper;
import com.example.adsportalbe.models.UserActionToken;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.repositories.UserRepository;
import com.example.adsportalbe.services.MailService;
import com.example.adsportalbe.services.SmsSenderService;
import com.example.adsportalbe.services.UserActionTokenService;
import com.example.adsportalbe.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserActionTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final SmsSenderService smsSenderService;
    private final UserMapper userMapper;

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    public Optional<User> findOptionalByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public Optional<User> findOptionalByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    @Transactional
    public User resetPassword(ResetPasswordRequestDto request) {
        UserActionToken token = tokenService.findToken(request.token());

        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token has expired");
        }

        if (token.isUsed()) {
            throw new IllegalArgumentException("Token has already been used");
        }

        if (!token.getType().equals(TokenType.PASSWORD_RESET)) {
            throw new IllegalArgumentException("Token is not a password reset token");
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
    public void sendPhoneVerification(SendPhoneVerificationRequestDto request, User user) {
        String phoneNumber = smsSenderService.normalizePhoneNumber(request.phoneNumber());
        User currentUser = findCurrentUser(user);

        findOptionalByPhoneNumber(phoneNumber)
                .filter(existing -> !existing.getId().equals(currentUser.getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("Phone number is already used.");
                });

        UserActionToken token = tokenService.createPhoneVerificationToken(currentUser, phoneNumber);
        String smsContent = "Your AllChat Ads Portal verification code is: " + token.getToken();
        smsSenderService.sendSMS(phoneNumber, smsContent);
    }

    @Override
    @Transactional
    public UserDto verifyPhone(VerifyPhoneRequestDto request, User user) {
        UserActionToken token = tokenService.findToken(request.verificationCode());

        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new ConflictException("Token has expired");
        }

        if (!token.getType().equals(TokenType.PHONE_VERIFICATION)) {
            throw new ConflictException("Token is not a phone verification token");
        }

        if (token.isUsed()) {
            throw new ConflictException("Token has already been used");
        }

        User currentUser = findCurrentUser(user);

        if (!token.getUser().getId().equals(currentUser.getId())) {
            throw new ConflictException("Token does not belong to the authenticated user");
        }

        findOptionalByPhoneNumber(token.getPhoneNumber())
                .filter(existing -> !existing.getId().equals(currentUser.getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("Phone number is already used.");
                });

        currentUser.setPhoneNumber(token.getPhoneNumber());
        currentUser.setPhoneNumberVerificationDate(Instant.now());

        token.setUsed(true);
        tokenService.save(token);

        return userMapper.toUserDto(save(currentUser));
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
    @Transactional
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

    @Override
    public void updateMarketingPreferences(UpdateMarketingPreferencesDto request, User user) {
        user.setSubscribedToMarketingEmails(request.getSubscribedToMarketingEmails());
        userRepository.save(user);
    }

    private User findCurrentUser(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + user.getId()));
    }
}

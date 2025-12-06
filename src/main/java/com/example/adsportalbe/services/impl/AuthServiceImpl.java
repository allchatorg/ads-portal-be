package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.dto.auth.*;
import com.example.adsportalbe.enums.Role;
import com.example.adsportalbe.models.UserActionToken;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.repositories.UserRepository;
import com.example.adsportalbe.services.AuthService;
import com.example.adsportalbe.services.MailService;
import com.example.adsportalbe.services.UserActionTokenService;
import com.example.adsportalbe.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final com.example.adsportalbe.security.JwtUtils jwtUtils;
    private final UserActionTokenService tokenService;
    private final MailService mailService;
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
                .verified(false) // User needs to verify email
                .build();

        user = userRepository.save(user);

        // Send verification email
        userService.sendEmailVerification(user);

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
        if (request.email() == null && request.username() == null) {
            throw new IllegalArgumentException("Email or username must be provided");
        }

        User user = request.email() != null ? userService.findByEmail(request.email())
                : userRepository.findByEmail(request.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserActionToken token = tokenService.createPasswordResetTokenForUser(user);
        mailService.sendResetPasswordEmail(user, token.getToken());
    }

    @Override
    @Transactional
    public User resetPassword(ResetPasswordRequestDto request) {
        return userService.resetPassword(request);
    }
}

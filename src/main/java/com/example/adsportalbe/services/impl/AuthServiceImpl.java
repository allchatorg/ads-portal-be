package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.dto.auth.AuthResponseDto;
import com.example.adsportalbe.dto.auth.LoginRequestDto;
import com.example.adsportalbe.dto.auth.RegisterRequestDto;
import com.example.adsportalbe.enums.Role;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.repositories.UserRepository;
import com.example.adsportalbe.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

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
                .verified(true) // User needs to verify email
                .build();

        user = userRepository.save(user);

        // Immediately authenticate the newly registered user and create a session
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authenticationToken);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        return AuthResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(session.getId())
                .tokenType("SESSION")
                .expiresIn((long) session.getMaxInactiveInterval())
                .message("Registration successful. You are now logged in.")
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

        // Set authentication in security context
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);

        // Create new session and add security context
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        // Get authenticated user
        User user = (User) authentication.getPrincipal();

        return AuthResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(session.getId())
                .tokenType("SESSION")
                .expiresIn((long) session.getMaxInactiveInterval())
                .message("Login successful")
                .build();
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // Clear security context
        SecurityContextHolder.clearContext();

        // Invalidate session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}

package com.example.adsportalbe.dto.auth;

import com.example.adsportalbe.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    // Authentication token information (for session-based or JWT auth)
    private String accessToken;    // In current implementation, this will be the HTTP session ID after login
    private String tokenType;      // e.g., "SESSION" or "Bearer"
    private Long expiresIn;        // seconds until expiration
    private String message;
}

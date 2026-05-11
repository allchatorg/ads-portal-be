package com.example.adsportalbe.dto;

import com.example.adsportalbe.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Instant phoneNumberVerificationDate;
    private boolean emailVerified;
    private boolean subscribedToMarketingEmails;
    private Role role;
}

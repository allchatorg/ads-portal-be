package com.example.adsportalbe.services;

import com.example.adsportalbe.dto.UserDto;
import com.example.adsportalbe.dto.auth.*;
import com.example.adsportalbe.models.identity.User;

import java.util.Optional;

public interface UserService {

    User findByEmail(String email);

    Optional<User> findOptionalByEmail(String email);

    Optional<User> findOptionalByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    User resetPassword(ResetPasswordRequestDto request);

    void sendEmailVerification(User user);

    User verifyEmail(String token);

    void sendPhoneVerification(SendPhoneVerificationRequestDto request, User user);

    UserDto verifyPhone(VerifyPhoneRequestDto request, User user);

    User save(User user);

    void changePassword(ChangePasswordRequestDto request, User user);

    UserDto getCurrentUser(User user);

    void requestEmailUpdate(RequestEmailUpdateDto request, User user);

    User verifyEmailUpdate(VerifyEmailUpdateDto request, User user);

    void updateMarketingPreferences(UpdateMarketingPreferencesDto request, User user);
}

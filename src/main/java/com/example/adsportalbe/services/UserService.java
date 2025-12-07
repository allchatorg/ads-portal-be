package com.example.adsportalbe.services;

import com.example.adsportalbe.dto.UserDto;
import com.example.adsportalbe.dto.auth.*;
import com.example.adsportalbe.models.identity.User;

public interface UserService {

    User findByEmail(String email);

    boolean existsByEmail(String email);

    User resetPassword(ResetPasswordRequestDto request);

    void sendEmailVerification(User user);

    User verifyEmail(String token);

    User save(User user);

    void changePassword(ChangePasswordRequestDto request, User user);

    UserDto getCurrentUser(User user);

    void requestEmailUpdate(RequestEmailUpdateDto request, User user);

    User verifyEmailUpdate(VerifyEmailUpdateDto request, User user);

    void updateMarketingPreferences(UpdateMarketingPreferencesDto request, User user);
}

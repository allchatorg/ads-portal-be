package com.example.adsportalbe.services;

import com.example.adsportalbe.models.UserActionToken;
import com.example.adsportalbe.models.identity.User;

public interface UserActionTokenService {

    UserActionToken createPasswordResetTokenForUser(User user);

    UserActionToken createEmailVerificationTokenForUser(User user);

    UserActionToken createPhoneVerificationToken(User currentUser, String number);

    UserActionToken findToken(String token);

    UserActionToken save(UserActionToken userActionToken);
}

package com.example.adsportalbe.services;

import com.example.adsportalbe.models.identity.User;

public interface MailService {
    void sendSimpleMail(String to, String subject, String text);

    void sendResetPasswordEmail(User user, String token);

    void sendVerificationEmail(User user, String code);

    void sendEmailUpdateVerification(String newEmail, String code);
}

package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.services.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    @Override
    public void sendSimpleMail(String to, String subject, String text) {
    }

    @Override
    public void sendResetPasswordEmail(User user, String token) {
    }

    @Override
    public void sendVerificationEmail(User user, String code) {
    }

    @Override
    public void sendEmailUpdateVerification(String newEmail, String code) {
    }

    @Override
    public void sendAdRejectionEmail(User user, String adTitle, String rejectionReason) {
    }

    @Override
    public void sendAdApprovalEmail(User user, String adTitle) {
    }
}

package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.services.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String FROM_NAME = "AllChat Team";
    @Value("${app.FRONT_END.URL}")
    private String FRONT_END_URL;
    @Value("${SPRING_MAIL_USERNAME}")
    private String MAIL_USERNAME;

    @Override
    public void sendSimpleMail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        message.setFrom(MAIL_USERNAME);
        mailSender.send(message);
    }

    @Override
    public void sendResetPasswordEmail(User user, String token) {
        String resetLink = FRONT_END_URL + "/reset-password?token=" + token;

        Context context = new Context();
        context.setVariable("name", user.getFirstName());
        context.setVariable("resetLink", resetLink);

        String htmlContent = templateEngine.process("RESET_PASSWORD_TEMPLATE", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

            helper.setTo(user.getEmail());
            helper.setSubject("Password Reset");
            helper.setText(htmlContent, true);
            helper.setFrom(MAIL_USERNAME, FROM_NAME);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send reset password email", (Throwable) e);
        }
    }

    @Override
    public void sendVerificationEmail(User user, String code) {
        Context context = new Context();
        context.setVariable("name", user.getFirstName());
        context.setVariable("verificationCode", code);

        String htmlContent = templateEngine.process("EMAIL_VERIFICATION_TEMPLATE", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

            helper.setTo(user.getEmail());
            helper.setSubject("Email Verification");
            helper.setText(htmlContent, true);
            helper.setFrom(MAIL_USERNAME, FROM_NAME);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    public void sendEmailUpdateVerification(String newEmail, String code) {
        Context context = new Context();
        context.setVariable("verificationCode", code);
        context.setVariable("newEmail", newEmail);

        String htmlContent = templateEngine.process("EMAIL_UPDATE_TEMPLATE", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

            helper.setTo(newEmail);
            helper.setSubject("Email Update Verification");
            helper.setText(htmlContent, true);
            helper.setFrom(MAIL_USERNAME, FROM_NAME);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send email update verification", e);
        }
    }

    @Override
    public void sendAdRejectionEmail(User user, String adTitle, String rejectionReason) {
        Context context = new Context();
        context.setVariable("name", user.getFirstName());
        context.setVariable("adTitle", adTitle);
        context.setVariable("rejectionReason", rejectionReason);

        String htmlContent = templateEngine.process("AD_REJECTION_TEMPLATE", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

            helper.setTo(user.getEmail());
            helper.setSubject("Ad Rejected - " + adTitle);
            helper.setText(htmlContent, true);
            helper.setFrom(MAIL_USERNAME, FROM_NAME);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send ad rejection email", e);
        }
    }
}

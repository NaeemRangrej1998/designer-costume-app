package com.designer.costume.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.auth.password-reset-delivery",
        havingValue = "smtp",
        matchIfMissing = true)
public class SmtpPasswordResetMailer implements PasswordResetMailer {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpPasswordResetMailer(
            JavaMailSender mailSender,
            @Value("${app.auth.mail-from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendResetLink(String email, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Reset your Designer Costume Studio password");
        message.setText("Use this link to reset your password before it expires:\n" + resetLink);
        mailSender.send(message);
    }
}

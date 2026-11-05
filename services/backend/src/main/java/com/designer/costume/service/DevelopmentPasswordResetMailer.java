package com.designer.costume.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.auth.password-reset-delivery", havingValue = "development")
public class DevelopmentPasswordResetMailer implements PasswordResetMailer {

    @Override
    public void sendResetLink(String email, String resetLink) {
    }
}

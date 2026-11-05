package com.designer.costume.service;

public interface PasswordResetMailer {

    void sendResetLink(String email, String resetLink);
}

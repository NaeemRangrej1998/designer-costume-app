package com.designer.costume.service;

import com.designer.costume.dto.request.ResetPasswordRequest;
import com.designer.costume.dto.response.ForgotPasswordResponse;
import com.designer.costume.entity.user.PasswordResetTokenEntity;
import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.exception.InvalidTokenException;
import com.designer.costume.repository.user.PasswordResetTokenRepository;
import com.designer.costume.repository.user.RefreshTokenRepository;
import com.designer.costume.repository.user.UserRepository;
import com.designer.costume.security.TokenHashing;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String GENERIC_MESSAGE =
            "If the account exists, password reset instructions will be sent";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailer passwordResetMailer;
    private final long expirationMillis;
    private final String resetBaseUrl;
    private final String deliveryMode;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetMailer passwordResetMailer,
            @Value("${app.auth.password-reset-expiration}") long expirationMillis,
            @Value("${app.auth.password-reset-base-url}") String resetBaseUrl,
            @Value("${app.auth.password-reset-delivery}") String deliveryMode) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetMailer = passwordResetMailer;
        if (expirationMillis <= 0) {
            throw new IllegalStateException("app.auth.password-reset-expiration must be positive");
        }
        this.expirationMillis = expirationMillis;
        this.resetBaseUrl = resetBaseUrl;
        this.deliveryMode = deliveryMode;
    }

    @Transactional
    public ForgotPasswordResponse requestReset(String email) {
        var user = userRepository.findByEmailIgnoreCase(email.trim());
        if (user.isEmpty() || !user.get().isActive()) {
            return new ForgotPasswordResponse(GENERIC_MESSAGE, null);
        }
        var lockedUser = userRepository.findByIdForUpdate(user.get().getId());
        if (lockedUser.isEmpty() || !lockedUser.get().isActive()) {
            return new ForgotPasswordResponse(GENERIC_MESSAGE, null);
        }

        String rawToken = createToken();
        Instant now = Instant.now();
        passwordResetTokenRepository.invalidateActiveForUser(lockedUser.get().getId(), now);

        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.setUser(lockedUser.get());
        entity.setTokenHash(TokenHashing.sha256(rawToken));
        entity.setExpiresAt(now.plusMillis(expirationMillis));
        passwordResetTokenRepository.save(entity);

        String link = resetBaseUrl + (resetBaseUrl.contains("?") ? "&" : "?")
                + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        try {
            passwordResetMailer.sendResetLink(lockedUser.get().getEmail(), link);
        } catch (MailException exception) {
            LOGGER.warn("Password reset email delivery failed");
        }
        return new ForgotPasswordResponse(
                GENERIC_MESSAGE,
                "development".equals(deliveryMode) ? rawToken : null);
    }

    @Transactional
    public void reset(ResetPasswordRequest request) {
        String hash = TokenHashing.sha256(request.token());
        PasswordResetTokenEntity candidate = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(InvalidTokenException::new);
        UserEntity lockedUser = userRepository.findByIdForUpdate(candidate.getUser().getId())
                .filter(UserEntity::isActive)
                .orElseThrow(InvalidTokenException::new);
        PasswordResetTokenEntity token = passwordResetTokenRepository.findByTokenHashForUpdate(hash)
                .filter(current -> TokenHashing.constantTimeEquals(current.getTokenHash(), hash)
                        && current.getUser().getId().equals(lockedUser.getId()))
                .orElseThrow(InvalidTokenException::new);
        Instant now = Instant.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new InvalidTokenException();
        }

        lockedUser.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        passwordResetTokenRepository.invalidateActiveForUser(lockedUser.getId(), now);
        refreshTokenRepository.revokeAllForUser(lockedUser.getId(), now);
        userRepository.save(lockedUser);
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

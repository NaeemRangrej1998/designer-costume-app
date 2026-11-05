package com.designer.costume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.repository.user.PasswordResetTokenRepository;
import com.designer.costume.repository.user.RefreshTokenRepository;
import com.designer.costume.repository.user.UserRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class PasswordResetServiceLoggingTest {

    @Test
    void smtpFailureDoesNotLogResetTokenOrResetLink() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordResetTokenRepository resetTokenRepository = mock(PasswordResetTokenRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordResetMailer mailer = mock(PasswordResetMailer.class);
        UserEntity user = new UserEntity();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setFullName("Logging Test");
        user.setEmail("logging@example.test");
        when(userRepository.findByEmailIgnoreCase("logging@example.test"))
                .thenReturn(Optional.of(user));
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        AtomicReference<String> resetLinkReference = new AtomicReference<>();
        doAnswer(invocation -> {
            String link = invocation.getArgument(1);
            resetLinkReference.set(link);
            throw new MailSendException(link);
        }).when(mailer).sendResetLink(anyString(), anyString());

        PasswordResetService service = new PasswordResetService(
                userRepository,
                resetTokenRepository,
                refreshTokenRepository,
                passwordEncoder,
                mailer,
                900_000,
                "https://example.test/reset",
                "smtp");
        Logger logger = (Logger) LoggerFactory.getLogger(PasswordResetService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            var result = service.requestReset("logging@example.test");
            String resetLink = resetLinkReference.get();
            String rawToken = resetLink.substring(resetLink.indexOf("token=") + "token=".length());
            assertThat(result.developmentResetToken()).isNull();
            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly("Password reset email delivery failed")
                    .doesNotContain(resetLink, rawToken);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}

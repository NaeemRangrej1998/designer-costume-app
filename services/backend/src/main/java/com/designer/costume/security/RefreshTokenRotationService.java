package com.designer.costume.security;

import com.designer.costume.entity.user.RefreshTokenEntity;
import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.exception.InvalidTokenException;
import com.designer.costume.repository.user.RefreshTokenRepository;
import com.designer.costume.repository.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenRotationService {

    private static final Duration CONCURRENT_RETRY_GRACE_PERIOD = Duration.ofSeconds(5);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public RefreshTokenRotationService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        Instant requestStartedAt = Instant.now();
        var claims = jwtService.parseForRevocation(rawToken, JwtTokenType.REFRESH);
        UUID userId = UUID.fromString(claims.getSubject());
        UserEntity user = userRepository.findByIdForUpdate(userId)
                .filter(UserEntity::isActive)
                .orElseThrow(InvalidTokenException::new);
        String hash = TokenHashing.sha256(rawToken);
        RefreshTokenEntity oldToken = refreshTokenRepository.findByTokenHashForUpdate(hash)
                .orElseThrow(InvalidTokenException::new);
        if (!TokenHashing.constantTimeEquals(oldToken.getTokenHash(), hash)
                || !oldToken.getUser().getId().equals(userId)) {
            throw new InvalidTokenException();
        }

        if (oldToken.getRevokedAt() != null) {
            if (!oldToken.getRevokedAt().plus(CONCURRENT_RETRY_GRACE_PERIOD)
                    .isAfter(requestStartedAt)) {
                refreshTokenRepository.revokeActiveFamily(oldToken.getFamilyId(), Instant.now());
            }
            return RotationResult.reusedResult();
        }

        Instant now = Instant.now();
        if (!oldToken.getExpiresAt().isAfter(now)
                || !claims.getExpiration().toInstant().isAfter(now)) {
            throw new InvalidTokenException();
        }

        user = userRepository.findWithAuthoritiesById(userId)
                .filter(UserEntity::isActive)
                .orElseThrow(InvalidTokenException::new);

        UUID replacementId = UUID.randomUUID();
        String replacement = jwtService.generate(
                user.getId(),
                JwtTokenType.REFRESH,
                replacementId,
                jwtService.refreshExpirationMillis());

        RefreshTokenEntity newToken = new RefreshTokenEntity();
        newToken.setFamilyId(oldToken.getFamilyId());
        newToken.setUser(user);
        newToken.setTokenHash(TokenHashing.sha256(replacement));
        newToken.setExpiresAt(now.plusMillis(jwtService.refreshExpirationMillis()));
        refreshTokenRepository.saveAndFlush(newToken);

        oldToken.setRevokedAt(now);
        oldToken.setReplacedBy(newToken.getId());
        refreshTokenRepository.save(oldToken);
        return RotationResult.rotated(user.getId(), replacement);
    }

    @Transactional
    public void revokeFamily(String rawToken) {
        var claims = jwtService.parseForRevocation(rawToken, JwtTokenType.REFRESH);
        UUID userId = UUID.fromString(claims.getSubject());
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(InvalidTokenException::new);
        String hash = TokenHashing.sha256(rawToken);
        RefreshTokenEntity token = refreshTokenRepository.findByTokenHashForUpdate(hash)
                .orElseThrow(InvalidTokenException::new);
        if (!TokenHashing.constantTimeEquals(token.getTokenHash(), hash)
                || !token.getUser().getId().equals(userId)) {
            throw new InvalidTokenException();
        }
        refreshTokenRepository.revokeActiveFamily(token.getFamilyId(), Instant.now());
    }

    public record RotationResult(boolean reused, UUID userId, String refreshToken) {

        static RotationResult reusedResult() {
            return new RotationResult(true, null, null);
        }

        static RotationResult rotated(UUID userId, String refreshToken) {
            return new RotationResult(false, userId, refreshToken);
        }
    }
}

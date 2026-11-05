package com.designer.costume.security;

import com.designer.costume.entity.user.RefreshTokenEntity;
import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.exception.InvalidTokenException;
import com.designer.costume.repository.user.RefreshTokenRepository;
import com.designer.costume.repository.user.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenRotationService rotationService;
    private final UserRepository userRepository;

    public TokenService(
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenRotationService rotationService,
            UserRepository userRepository) {
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.rotationService = rotationService;
        this.userRepository = userRepository;
    }

    @Transactional
    public String createRefreshToken(UserEntity user) {
        UserEntity lockedUser = userRepository.findByIdForUpdate(user.getId())
                .filter(UserEntity::isActive)
                .orElseThrow(InvalidTokenException::new);
        UUID familyId = UUID.randomUUID();
        String rawToken = jwtService.generate(
                lockedUser.getId(),
                JwtTokenType.REFRESH,
                UUID.randomUUID(),
                jwtService.refreshExpirationMillis());

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setFamilyId(familyId);
        entity.setUser(lockedUser);
        entity.setTokenHash(TokenHashing.sha256(rawToken));
        entity.setExpiresAt(Instant.now().plusMillis(jwtService.refreshExpirationMillis()));
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    public RotatedToken rotate(String refreshToken) {
        RefreshTokenRotationService.RotationResult result = rotationService.rotate(refreshToken);
        if (result.reused()) {
            throw new InvalidTokenException();
        }
        return new RotatedToken(
                result.userId(),
                result.refreshToken(),
                jwtService.generate(
                        result.userId(),
                        JwtTokenType.ACCESS,
                        UUID.randomUUID(),
                        jwtService.accessExpirationMillis()));
    }

    public void revoke(String refreshToken) {
        rotationService.revokeFamily(refreshToken);
    }

    public long accessExpirationSeconds() {
        return jwtService.accessExpirationMillis() / 1000;
    }

    public long refreshExpirationSeconds() {
        return jwtService.refreshExpirationMillis() / 1000;
    }

    public record RotatedToken(UUID userId, String refreshToken, String accessToken) {
    }
}

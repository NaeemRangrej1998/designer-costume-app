package com.designer.costume.repository.user;

import com.designer.costume.entity.user.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT token FROM RefreshTokenEntity token WHERE token.tokenHash = :tokenHash")
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshTokenEntity token
            SET token.revokedAt = :revokedAt
            WHERE token.familyId = :familyId AND token.revokedAt IS NULL
            """)
    int revokeActiveFamily(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("""
            UPDATE RefreshTokenEntity token
            SET token.revokedAt = :revokedAt
            WHERE token.user.id = :userId AND token.revokedAt IS NULL
            """)
    int revokeAllForUser(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

    List<RefreshTokenEntity> findAllByUserIdAndRevokedAtIsNull(UUID userId);
}

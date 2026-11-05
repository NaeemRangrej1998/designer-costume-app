package com.designer.costume.repository.user;

import com.designer.costume.entity.user.PasswordResetTokenEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT token FROM PasswordResetTokenEntity token WHERE token.tokenHash = :tokenHash")
    Optional<PasswordResetTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            UPDATE PasswordResetTokenEntity token
            SET token.usedAt = :usedAt
            WHERE token.user.id = :userId AND token.usedAt IS NULL
            """)
    int invalidateActiveForUser(@Param("userId") UUID userId, @Param("usedAt") Instant usedAt);
}

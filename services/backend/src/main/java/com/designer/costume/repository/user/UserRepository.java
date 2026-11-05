package com.designer.costume.repository.user;

import com.designer.costume.entity.user.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    @Query("""
            SELECT COUNT(DISTINCT user.id)
            FROM UserEntity user
            JOIN user.roles role
            WHERE user.active = true AND role.name = 'CUSTOMER'
            """)
    long countActiveCustomers();

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("SELECT DISTINCT user FROM UserEntity user WHERE LOWER(user.email) = LOWER(:email)")
    Optional<UserEntity> findWithAuthoritiesByEmail(@Param("email") String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("SELECT DISTINCT user FROM UserEntity user WHERE user.id = :id")
    Optional<UserEntity> findWithAuthoritiesById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT user FROM UserEntity user WHERE user.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") UUID id);
}

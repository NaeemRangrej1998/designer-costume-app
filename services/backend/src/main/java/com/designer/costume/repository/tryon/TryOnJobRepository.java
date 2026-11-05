package com.designer.costume.repository.tryon;

import com.designer.costume.entity.tryon.TryOnJobEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TryOnJobRepository extends JpaRepository<TryOnJobEntity, UUID> {

    List<TryOnJobEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}

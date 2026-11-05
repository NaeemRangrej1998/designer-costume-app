package com.designer.costume.repository.favorite;

import com.designer.costume.entity.favorite.FavoriteEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, UUID> {

    boolean existsByUserIdAndCostumeId(UUID userId, UUID costumeId);

    List<FavoriteEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}

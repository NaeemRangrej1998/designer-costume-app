package com.designer.costume.repository.user;

import com.designer.costume.entity.user.UserPhotoEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPhotoRepository extends JpaRepository<UserPhotoEntity, UUID> {

    List<UserPhotoEntity> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);
}

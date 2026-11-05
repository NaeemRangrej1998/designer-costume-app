package com.designer.costume.repository.variant;

import com.designer.costume.entity.variant.ColorEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColorRepository extends JpaRepository<ColorEntity, UUID> {

    Optional<ColorEntity> findByNameIgnoreCase(String name);
}

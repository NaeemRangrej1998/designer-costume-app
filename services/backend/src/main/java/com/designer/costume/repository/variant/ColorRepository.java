package com.designer.costume.repository.variant;

import com.designer.costume.entity.variant.ColorEntity;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

public interface ColorRepository extends JpaRepository<ColorEntity, UUID> {

    Optional<ColorEntity> findByNameIgnoreCase(String name);

    List<ColorEntity> findAllByOrderByNameAsc();
}

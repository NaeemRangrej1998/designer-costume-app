package com.designer.costume.repository.variant;

import com.designer.costume.entity.variant.SizeEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeRepository extends JpaRepository<SizeEntity, UUID> {

    Optional<SizeEntity> findByNameIgnoreCase(String name);
}

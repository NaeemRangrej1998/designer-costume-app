package com.designer.costume.repository.costume;

import com.designer.costume.entity.costume.CostumeImageEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostumeImageRepository extends JpaRepository<CostumeImageEntity, UUID> {

    List<CostumeImageEntity> findAllByCostumeIdOrderByDisplayOrderAsc(UUID costumeId);

    java.util.Optional<CostumeImageEntity> findByIdAndCostumeId(UUID id, UUID costumeId);

    int countByCostumeId(UUID costumeId);
}

package com.designer.costume.repository.tryon;

import com.designer.costume.entity.tryon.TryOnResultEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TryOnResultRepository extends JpaRepository<TryOnResultEntity, UUID> {

    List<TryOnResultEntity> findAllByTryOnJobIdOrderByDisplayOrderAsc(UUID tryOnJobId);
}

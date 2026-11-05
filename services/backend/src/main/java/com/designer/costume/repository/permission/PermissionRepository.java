package com.designer.costume.repository.permission;

import com.designer.costume.entity.permission.PermissionEntity;
import java.util.Optional;
import java.util.List;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    Optional<PermissionEntity> findByName(String name);

    List<PermissionEntity> findAllByOrderByModuleAscActionAsc();

    List<PermissionEntity> findAllByNameIn(Collection<String> names);
}

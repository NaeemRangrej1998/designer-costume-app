package com.designer.costume.repository.role;

import com.designer.costume.entity.role.RoleEntity;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByName(String name);

    @EntityGraph(attributePaths = "permissions")
    List<RoleEntity> findAllByOrderByNameAsc();
}

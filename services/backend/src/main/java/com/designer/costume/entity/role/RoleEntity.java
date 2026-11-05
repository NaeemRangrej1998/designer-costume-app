package com.designer.costume.entity.role;

import com.designer.costume.entity.common.AuditedEntity;
import com.designer.costume.entity.permission.PermissionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class RoleEntity extends AuditedEntity {

    @Column(name = "name", nullable = false, length = 80)
    @NotBlank
    @Size(max = 80)
    private String name;

    @Column(name = "description", length = 500)
    @Size(max = 500)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "permission_id", nullable = false))
    private Set<PermissionEntity> permissions = new HashSet<>();
}

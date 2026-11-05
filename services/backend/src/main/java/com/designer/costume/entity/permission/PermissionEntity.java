package com.designer.costume.entity.permission;

import com.designer.costume.entity.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class PermissionEntity extends AuditedEntity {

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank
    @Size(max = 100)
    private String name;

    @Column(name = "description", length = 500)
    @Size(max = 500)
    private String description;

    @Column(name = "module", nullable = false, length = 60)
    @NotBlank
    @Size(max = 60)
    private String module;

    @Column(name = "action", nullable = false, length = 40)
    @NotBlank
    @Size(max = 40)
    private String action;
}

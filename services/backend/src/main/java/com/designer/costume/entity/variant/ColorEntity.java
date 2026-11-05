package com.designer.costume.entity.variant;

import com.designer.costume.entity.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "colors")
@Getter
@Setter
@NoArgsConstructor
public class ColorEntity extends AuditedEntity {

    @Column(name = "name", nullable = false, length = 80)
    @NotBlank
    @Size(max = 80)
    private String name;

    @Column(name = "hex_code", nullable = false, length = 7)
    @NotBlank
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    private String hexCode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

package com.designer.costume.entity.user;

import com.designer.costume.entity.common.AuditedEntity;
import com.designer.costume.entity.role.RoleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends AuditedEntity {

    @Column(name = "full_name", nullable = false, length = 150)
    @NotBlank
    @Size(max = 150)
    private String fullName;

    @Column(name = "email", nullable = false, length = 320)
    @NotBlank
    @Email
    @Size(max = 320)
    private String email;

    @Column(name = "phone_number", length = 32)
    @Size(max = 32)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, length = 255)
    @NotBlank
    @Size(max = 255)
    private String passwordHash;

    @Column(name = "profile_image_url", length = 2048)
    @Size(max = 2048)
    private String profileImage;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "role_id", nullable = false))
    private Set<RoleEntity> roles = new HashSet<>();

    @PrePersist
    @PreUpdate
    protected void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}

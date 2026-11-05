package com.designer.costume.entity.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class AuditedEntity extends CreatedAtEntity {

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void initializeUpdatedAt() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void updateTimestamp() {
        updatedAt = Instant.now();
    }

    protected AuditedEntity() {
    }
}

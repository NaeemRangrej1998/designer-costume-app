package com.designer.costume.entity.tryon;

import com.designer.costume.entity.common.CreatedAtEntity;
import com.designer.costume.entity.costume.CostumeEntity;
import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.entity.user.UserPhotoEntity;
import com.designer.costume.entity.variant.CostumeVariantEntity;
import com.designer.costume.enums.TryOnJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "try_on_jobs")
@Getter
@Setter
@NoArgsConstructor
public class TryOnJobEntity extends CreatedAtEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_photo_id", nullable = false)
    @NotNull
    private UserPhotoEntity userPhoto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "costume_id", nullable = false)
    @NotNull
    private CostumeEntity costume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "costume_variant_id")
    private CostumeVariantEntity costumeVariant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    private TryOnJobStatus status = TryOnJobStatus.PENDING;

    @Column(name = "ai_model_name", length = 150)
    @Size(max = 150)
    private String aiModelName;

    @Column(name = "ai_model_version", length = 100)
    @Size(max = 100)
    private String aiModelVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_metadata", nullable = false, columnDefinition = "jsonb")
    @NotNull
    private Map<String, Object> requestMetadata = new HashMap<>();

    @Column(name = "error_message", length = 4000)
    @Size(max = 4000)
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "tryOnJob", fetch = FetchType.LAZY)
    private List<TryOnResultEntity> results = new ArrayList<>();
}

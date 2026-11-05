package com.designer.costume.entity.tryon;

import com.designer.costume.entity.common.CreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "try_on_results")
@Getter
@Setter
@NoArgsConstructor
public class TryOnResultEntity extends CreatedAtEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "try_on_job_id", nullable = false)
    @NotNull
    private TryOnJobEntity tryOnJob;

    @Column(name = "result_image_url", nullable = false, length = 2048)
    @NotBlank
    @Size(max = 2048)
    private String resultImageUrl;

    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    @NotBlank
    @Size(max = 512)
    private String storageKey;

    @Column(name = "display_order", nullable = false)
    @PositiveOrZero
    private int displayOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;
}

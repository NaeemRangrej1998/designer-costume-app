package com.designer.costume.entity.costume;

import com.designer.costume.entity.common.CreatedAtEntity;
import com.designer.costume.enums.CostumeImageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "costume_images")
@Getter
@Setter
@NoArgsConstructor
public class CostumeImageEntity extends CreatedAtEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "costume_id", nullable = false)
    @NotNull
    private CostumeEntity costume;

    @Column(name = "image_url", nullable = false, length = 2048)
    @NotBlank
    @Size(max = 2048)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20)
    @NotNull
    private CostumeImageType imageType;

    @Column(name = "display_order", nullable = false)
    @PositiveOrZero
    private int displayOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;
}

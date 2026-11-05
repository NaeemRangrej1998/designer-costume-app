package com.designer.costume.entity.costume;

import com.designer.costume.entity.category.CategoryEntity;
import com.designer.costume.entity.common.AuditedEntity;
import com.designer.costume.entity.variant.CostumeVariantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "costumes")
@Getter
@Setter
@NoArgsConstructor
public class CostumeEntity extends AuditedEntity {

    @Column(name = "name", nullable = false, length = 200)
    @NotBlank
    @Size(max = 200)
    private String name;

    @Column(name = "description", length = 5000)
    @Size(max = 5000)
    private String description;

    @Column(name = "sku", nullable = false, length = 80)
    @NotBlank
    @Size(max = 80)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull
    private CategoryEntity category;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal basePrice;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "costume", fetch = FetchType.LAZY)
    private List<CostumeImageEntity> images = new ArrayList<>();

    @OneToMany(mappedBy = "costume", fetch = FetchType.LAZY)
    private List<CostumeVariantEntity> variants = new ArrayList<>();
}

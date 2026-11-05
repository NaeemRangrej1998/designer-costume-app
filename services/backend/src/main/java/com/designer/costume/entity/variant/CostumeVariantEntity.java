package com.designer.costume.entity.variant;

import com.designer.costume.entity.common.AuditedEntity;
import com.designer.costume.entity.costume.CostumeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "costume_variants")
@Getter
@Setter
@NoArgsConstructor
public class CostumeVariantEntity extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "costume_id", nullable = false)
    @NotNull
    private CostumeEntity costume;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "color_id", nullable = false)
    @NotNull
    private ColorEntity color;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "size_id", nullable = false)
    @NotNull
    private SizeEntity size;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    @PositiveOrZero
    private int stockQuantity;

    @Column(name = "sku", nullable = false, length = 100)
    @NotBlank
    @Size(max = 100)
    private String sku;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Column(name = "image_url", length = 2048)
    @Size(max = 2048)
    private String imageUrl;
}

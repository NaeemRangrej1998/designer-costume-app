package com.designer.costume.repository.variant;

import com.designer.costume.dto.response.CostumeVariantResponse;
import com.designer.costume.entity.variant.CostumeVariantEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CostumeVariantRepository extends JpaRepository<CostumeVariantEntity, UUID> {

    @Query("""
            SELECT COUNT(variant.id)
            FROM CostumeVariantEntity variant
            JOIN variant.costume costume
            JOIN variant.color color
            JOIN variant.size size
            WHERE variant.available = true
              AND costume.active = true
              AND color.active = true
              AND size.active = true
            """)
    long countAvailableVariantsForActiveCatalog();

    Optional<CostumeVariantEntity> findBySku(String sku);

    Optional<CostumeVariantEntity> findByCostumeIdAndColorIdAndSizeId(
            UUID costumeId, UUID colorId, UUID sizeId);

    @Query("""
            SELECT new com.designer.costume.dto.response.CostumeVariantResponse(
                variant.id, variant.costume.id, color.name, color.hexCode,
                size.name, size.displayOrder, variant.price, variant.available
            )
            FROM CostumeVariantEntity variant
            JOIN variant.color color
            JOIN variant.size size
            WHERE variant.costume.id = :costumeId
              AND variant.available = true
              AND color.active = true
              AND size.active = true
            ORDER BY color.name, size.displayOrder
            """)
    List<CostumeVariantResponse> findAvailableVariantsByCostumeId(
            @Param("costumeId") UUID costumeId);
}

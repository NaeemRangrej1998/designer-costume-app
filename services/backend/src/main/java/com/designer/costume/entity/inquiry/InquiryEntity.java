package com.designer.costume.entity.inquiry;

import com.designer.costume.entity.common.AuditedEntity;
import com.designer.costume.entity.costume.CostumeEntity;
import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.entity.variant.CostumeVariantEntity;
import com.designer.costume.enums.InquiryStatus;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Locale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inquiries")
@Getter
@Setter
@NoArgsConstructor
public class InquiryEntity extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "costume_id")
    private CostumeEntity costume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "costume_variant_id")
    private CostumeVariantEntity costumeVariant;

    @Column(name = "customer_name", nullable = false, length = 150)
    @NotBlank
    @Size(max = 150)
    private String customerName;

    @Column(name = "email", nullable = false, length = 320)
    @NotBlank
    @Email
    @Size(max = 320)
    private String email;

    @Column(name = "phone_number", length = 32)
    @Size(max = 32)
    private String phoneNumber;

    @Column(name = "message", nullable = false, length = 5000)
    @NotBlank
    @Size(max = 5000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    private InquiryStatus status = InquiryStatus.NEW;

    @PrePersist
    @PreUpdate
    protected void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}

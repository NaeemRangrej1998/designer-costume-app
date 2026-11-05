package com.designer.costume.entity.user;

import com.designer.costume.entity.common.CreatedAtEntity;
import com.designer.costume.enums.PhotoProcessingStatus;
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
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_photos")
@Getter
@Setter
@NoArgsConstructor
public class UserPhotoEntity extends CreatedAtEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private UserEntity user;

    @Column(name = "image_url", nullable = false, length = 2048)
    @NotBlank
    @Size(max = 2048)
    private String imageUrl;

    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    @NotBlank
    @Size(max = 512)
    private String storageKey;

    @Column(name = "original_file_name", nullable = false, length = 255)
    @NotBlank
    @Size(max = 255)
    private String originalFileName;

    @Column(name = "mime_type", nullable = false, length = 100)
    @NotBlank
    @Size(max = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    @Positive
    private long fileSize;

    @Column(name = "image_width")
    @Positive
    private Integer imageWidth;

    @Column(name = "image_height")
    @Positive
    private Integer imageHeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    @NotNull
    private PhotoProcessingStatus processingStatus = PhotoProcessingStatus.UPLOADED;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}

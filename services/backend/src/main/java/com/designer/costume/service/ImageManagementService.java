package com.designer.costume.service;

import com.designer.costume.dto.response.CostumeImageResponse;
import com.designer.costume.dto.response.UserPhotoResponse;
import com.designer.costume.entity.costume.CostumeEntity;
import com.designer.costume.entity.costume.CostumeImageEntity;
import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.entity.user.UserPhotoEntity;
import com.designer.costume.enums.CostumeImageType;
import com.designer.costume.enums.PhotoProcessingStatus;
import com.designer.costume.exception.BadRequestException;
import com.designer.costume.exception.ResourceNotFoundException;
import com.designer.costume.repository.costume.CostumeImageRepository;
import com.designer.costume.repository.costume.CostumeRepository;
import com.designer.costume.repository.user.UserPhotoRepository;
import com.designer.costume.repository.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageManagementService.class);
    private final CostumeRepository costumeRepository;
    private final CostumeImageRepository costumeImageRepository;
    private final UserRepository userRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final LocalImageStorageService storage;

    public ImageManagementService(
            CostumeRepository costumeRepository,
            CostumeImageRepository costumeImageRepository,
            UserRepository userRepository,
            UserPhotoRepository userPhotoRepository,
            LocalImageStorageService storage) {
        this.costumeRepository = costumeRepository;
        this.costumeImageRepository = costumeImageRepository;
        this.userRepository = userRepository;
        this.userPhotoRepository = userPhotoRepository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<CostumeImageResponse> getCostumeImages(UUID costumeId) {
        requireCostume(costumeId);
        return costumeImageRepository.findAllByCostumeIdOrderByDisplayOrderAsc(costumeId)
                .stream().map(this::toCostumeImageResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CostumeImageResponse> getPublicCostumeImages(UUID costumeId) {
        if (!costumeRepository.existsByIdAndActiveTrue(costumeId)) {
            throw new ResourceNotFoundException("Costume", costumeId);
        }
        return getCostumeImages(costumeId);
    }

    @Transactional
    public CostumeImageResponse uploadCostumeImage(UUID costumeId, MultipartFile upload) {
        CostumeEntity costume = requireCostume(costumeId);
        StoredImage stored = storage.store(upload);
        try {
            CostumeImageEntity image = new CostumeImageEntity();
            image.setCostume(costume);
            image.setStorageKey(stored.storageKey());
            image.setOriginalFileName(safeFileName(upload.getOriginalFilename()));
            image.setMimeType(stored.mimeType());
            image.setFileSize(stored.fileSize());
            image.setImageWidth(stored.imageWidth());
            image.setImageHeight(stored.imageHeight());
            image.setImageUrl(costumeImageContentUrl(costumeId, UUID.randomUUID()));
            image.setImageType(CostumeImageType.PRODUCT);
            image.setDisplayOrder(costumeImageRepository.countByCostumeId(costumeId));
            boolean firstImage = image.getDisplayOrder() == 0;
            image.setPrimary(firstImage);
            CostumeImageEntity saved = costumeImageRepository.save(image);
            saved.setImageUrl(costumeImageContentUrl(costumeId, saved.getId()));
            onRollback(() -> storage.delete(stored.storageKey()));
            return toCostumeImageResponse(saved);
        } catch (RuntimeException exception) {
            try {
                storage.delete(stored.storageKey());
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional
    public CostumeImageResponse setPrimaryCostumeImage(UUID costumeId, UUID imageId) {
        requireCostume(costumeId);
        CostumeImageEntity selected = findCostumeImage(costumeId, imageId);
        List<CostumeImageEntity> images =
                costumeImageRepository.findAllByCostumeIdOrderByDisplayOrderAsc(costumeId);
        images.forEach(image -> image.setPrimary(false));
        costumeImageRepository.flush();
        selected.setPrimary(true);
        return toCostumeImageResponse(selected);
    }

    @Transactional
    public List<CostumeImageResponse> reorderCostumeImages(UUID costumeId, List<UUID> imageIds) {
        requireCostume(costumeId);
        List<CostumeImageEntity> images =
                costumeImageRepository.findAllByCostumeIdOrderByDisplayOrderAsc(costumeId);
        if (imageIds == null || imageIds.stream().anyMatch(java.util.Objects::isNull)
                || imageIds.size() != images.size()
                || imageIds.stream().distinct().count() != images.size()
                || !images.stream().map(CostumeImageEntity::getId).collect(java.util.stream.Collectors.toSet())
                        .equals(new java.util.HashSet<>(imageIds))) {
            throw new BadRequestException("Image order must include every costume image exactly once");
        }
        var imageById = images.stream().collect(
                java.util.stream.Collectors.toMap(CostumeImageEntity::getId, image -> image));
        for (int order = 0; order < imageIds.size(); order++) {
            imageById.get(imageIds.get(order)).setDisplayOrder(order);
        }
        return imageIds.stream().map(imageById::get).map(this::toCostumeImageResponse).toList();
    }

    @Transactional
    public void deleteCostumeImage(UUID costumeId, UUID imageId) {
        requireCostume(costumeId);
        CostumeImageEntity image = findCostumeImage(costumeId, imageId);
        String storageKey = image.getStorageKey();
        boolean wasPrimary = image.isPrimary();
        costumeImageRepository.delete(image);
        List<CostumeImageEntity> remaining =
                costumeImageRepository.findAllByCostumeIdOrderByDisplayOrderAsc(costumeId).stream()
                        .filter(item -> !item.getId().equals(imageId)).toList();
        for (int order = 0; order < remaining.size(); order++) {
            CostumeImageEntity item = remaining.get(order);
            item.setDisplayOrder(order);
            if (wasPrimary && order == 0) {
                item.setPrimary(true);
            }
        }
        if (storageKey != null) {
            afterCommit(() -> storage.delete(storageKey));
        }
    }

    @Transactional(readOnly = true)
    public StoredImageContent getCostumeImageContent(UUID costumeId, UUID imageId, boolean activeOnly) {
        if (activeOnly && !costumeRepository.existsByIdAndActiveTrue(costumeId)) {
            throw new ResourceNotFoundException("Costume", costumeId);
        }
        requireCostume(costumeId);
        CostumeImageEntity image = findCostumeImage(costumeId, imageId);
        if (image.getStorageKey() == null) {
            throw new ResourceNotFoundException("Costume image", imageId);
        }
        return new StoredImageContent(storage.load(image.getStorageKey()), image.getMimeType());
    }

    @Transactional(readOnly = true)
    public List<UserPhotoResponse> getUserPhotos(UUID userId) {
        return userPhotoRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream().map(this::toUserPhotoResponse).toList();
    }

    @Transactional
    public UserPhotoResponse uploadUserPhoto(UUID userId, MultipartFile upload) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        StoredImage stored = storage.store(upload);
        try {
            UserPhotoEntity photo = new UserPhotoEntity();
            photo.setUser(user);
            photo.setStorageKey(stored.storageKey());
            photo.setOriginalFileName(safeFileName(upload.getOriginalFilename()));
            photo.setMimeType(stored.mimeType());
            photo.setFileSize(stored.fileSize());
            photo.setImageWidth(stored.imageWidth());
            photo.setImageHeight(stored.imageHeight());
            photo.setImageUrl("/api/v1/customer/photos/pending/content");
            photo.setProcessingStatus(PhotoProcessingStatus.UPLOADED);
            UserPhotoEntity saved = userPhotoRepository.save(photo);
            saved.setImageUrl("/api/v1/customer/photos/" + saved.getId() + "/content");
            onRollback(() -> storage.delete(stored.storageKey()));
            return toUserPhotoResponse(saved);
        } catch (RuntimeException exception) {
            try {
                storage.delete(stored.storageKey());
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public StoredImageContent getUserPhotoContent(UUID userId, UUID photoId) {
        UserPhotoEntity photo = userPhotoRepository.findByIdAndUserIdAndDeletedAtIsNull(photoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));
        return new StoredImageContent(storage.load(photo.getStorageKey()), photo.getMimeType());
    }

    @Transactional
    public void deleteUserPhoto(UUID userId, UUID photoId) {
        UserPhotoEntity photo = userPhotoRepository.findByIdAndUserIdAndDeletedAtIsNull(photoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));
        photo.setDeletedAt(Instant.now());
        afterCommit(() -> storage.delete(photo.getStorageKey()));
    }

    private CostumeEntity requireCostume(UUID id) {
        return costumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Costume", id));
    }

    private CostumeImageEntity findCostumeImage(UUID costumeId, UUID imageId) {
        return costumeImageRepository.findByIdAndCostumeId(imageId, costumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Costume image", imageId));
    }

    private CostumeImageResponse toCostumeImageResponse(CostumeImageEntity image) {
        return new CostumeImageResponse(
                image.getId(), image.getImageUrl(), image.getOriginalFileName(), image.getMimeType(),
                image.getFileSize(), image.getImageWidth(), image.getImageHeight(),
                image.getDisplayOrder(), image.isPrimary(), image.getCreatedAt());
    }

    private UserPhotoResponse toUserPhotoResponse(UserPhotoEntity photo) {
        return new UserPhotoResponse(
                photo.getId(), photo.getOriginalFileName(), photo.getMimeType(), photo.getFileSize(),
                photo.getImageWidth(), photo.getImageHeight(), photo.getProcessingStatus().name(),
                photo.getCreatedAt(), "/api/v1/customer/photos/" + photo.getId() + "/content");
    }

    private String costumeImageContentUrl(UUID costumeId, UUID imageId) {
        return "/api/v1/costumes/" + costumeId + "/images/" + imageId + "/content";
    }

    private String safeFileName(String fileName) {
        String name = fileName == null ? "image" : fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_").trim();
        if (name.isBlank()) {
            return "image";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private void onRollback(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        action.run();
                    } catch (RuntimeException exception) {
                        LOGGER.error("Could not remove image file after database rollback", exception);
                    }
                }
            }
        });
    }

    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    try {
                        action.run();
                    } catch (RuntimeException exception) {
                        LOGGER.error("Could not remove image file after database commit", exception);
                    }
                }
            }
        });
    }

    public record StoredImageContent(Resource resource, String mimeType) {
    }
}

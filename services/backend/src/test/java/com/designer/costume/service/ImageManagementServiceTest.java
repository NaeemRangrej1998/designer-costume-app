package com.designer.costume.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.designer.costume.entity.user.UserPhotoEntity;
import com.designer.costume.exception.ResourceNotFoundException;
import com.designer.costume.repository.costume.CostumeImageRepository;
import com.designer.costume.repository.costume.CostumeRepository;
import com.designer.costume.repository.user.UserPhotoRepository;
import com.designer.costume.repository.user.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ImageManagementServiceTest {

    @Mock
    private CostumeRepository costumeRepository;
    @Mock
    private CostumeImageRepository costumeImageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPhotoRepository userPhotoRepository;
    @Mock
    private LocalImageStorageService storage;
    @InjectMocks
    private ImageManagementService imageService;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void photoContentLookupRequiresTheAuthenticatedOwnerId() {
        UUID photoId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        when(userPhotoRepository.findByIdAndUserIdAndDeletedAtIsNull(photoId, otherCustomerId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> imageService.getUserPhotoContent(otherCustomerId, photoId));
        verify(userPhotoRepository).findByIdAndUserIdAndDeletedAtIsNull(photoId, otherCustomerId);
        verify(storage, never()).load(anyString());
    }

    @Test
    void photoContentIsLoadedOnlyForItsAuthenticatedOwner() {
        UUID photoId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UserPhotoEntity photo = new UserPhotoEntity();
        photo.setStorageKey(UUID.randomUUID() + ".png");
        photo.setMimeType("image/png");
        when(userPhotoRepository.findByIdAndUserIdAndDeletedAtIsNull(photoId, ownerId))
                .thenReturn(Optional.of(photo));
        when(storage.load(photo.getStorageKey())).thenReturn(new ByteArrayResource(new byte[] {1}));

        var result = imageService.getUserPhotoContent(ownerId, photoId);

        assertEquals("image/png", result.mimeType());
        verify(userPhotoRepository).findByIdAndUserIdAndDeletedAtIsNull(photoId, ownerId);
        verify(storage).load(photo.getStorageKey());
    }

    @Test
    void deletingPhotoMarksItDeletedAndRemovesItsFileOnlyAfterCommit() {
        UUID ownerId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UserPhotoEntity photo = new UserPhotoEntity();
        photo.setStorageKey(UUID.randomUUID() + ".jpg");
        when(userPhotoRepository.findByIdAndUserIdAndDeletedAtIsNull(photoId, ownerId))
                .thenReturn(Optional.of(photo));

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        imageService.deleteUserPhoto(ownerId, photoId);

        assertNotNull(photo.getDeletedAt());
        var synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);
        synchronization.afterCompletion(
                org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED);
        verify(storage).delete(photo.getStorageKey());
    }
}

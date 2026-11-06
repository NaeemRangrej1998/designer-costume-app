package com.designer.costume.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.designer.costume.exception.BadRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalImageStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    private LocalImageStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new LocalImageStorageService(temporaryDirectory.toString(), 1024);
    }

    @Test
    void storesValidJpegWithDetectedMetadataAndDeletesIt() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "jpg", bytes);
        var image = storage.store(new MockMultipartFile(
                "file", "../../portrait.jpg", "text/plain", bytes.toByteArray()));

        assertEquals("image/jpeg", image.mimeType());
        assertEquals(3, image.imageWidth());
        assertEquals(2, image.imageHeight());
        assertTrue(Files.exists(temporaryDirectory.resolve(image.storageKey())));
        storage.delete(image.storageKey());
        assertFalse(Files.exists(temporaryDirectory.resolve(image.storageKey())));
    }

    @Test
    void rejectsUnsupportedContentEvenWhenFilenameClaimsImage() {
        assertThrows(BadRequestException.class, () -> storage.store(new MockMultipartFile(
                "file", "image.png", "image/png", "not an image".getBytes())));
    }

    @Test
    void rejectsOversizedFileBeforeStoring() {
        assertThrows(BadRequestException.class, () -> storage.store(new MockMultipartFile(
                "file", "large.png", "image/png", new byte[1025])));
        assertFalse(Files.exists(temporaryDirectory.resolve(".staging"))
                && hasFiles(temporaryDirectory.resolve(".staging")));
    }

    @Test
    void rejectsUnsafeStorageKeys() {
        assertThrows(BadRequestException.class, () -> storage.load("../outside.jpg"));
    }

    private boolean hasFiles(Path directory) {
        try (var files = Files.list(directory)) {
            return files.findAny().isPresent();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

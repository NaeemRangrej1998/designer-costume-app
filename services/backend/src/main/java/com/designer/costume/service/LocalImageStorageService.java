package com.designer.costume.service;

import com.designer.costume.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalImageStorageService {

    private final Path root;
    private final long maxFileSize;

    public LocalImageStorageService(
            @Value("${app.storage.root:./storage}") String root,
            @Value("${app.storage.max-file-size:10485760}") long maxFileSize) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
    }

    public StoredImage store(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw new BadRequestException("Select an image to upload");
        }
        if (upload.getSize() > maxFileSize) {
            throw new BadRequestException("Image exceeds the maximum allowed size");
        }

        Path staging = null;
        try {
            Files.createDirectories(root.resolve(".staging"));
            staging = Files.createTempFile(root.resolve(".staging"), "upload-", ".tmp");
            long copied = copyWithLimit(upload.getInputStream(), staging);
            ImageFormat format = detectFormat(staging);
            String key = UUID.randomUUID() + "." + format.extension();
            Path destination = resolveKey(key);
            Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE);
            return new StoredImage(
                    key, format.mimeType(), copied, format.extension(),
                    format.width(), format.height());
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store uploaded image", exception);
        } finally {
            if (staging != null) {
                try {
                    Files.deleteIfExists(staging);
                } catch (IOException exception) {
                    throw new IllegalStateException("Could not clean image upload staging file", exception);
                }
            }
        }
    }

    public Resource load(String key) {
        try {
            Path path = resolveKey(key);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Stored image is missing");
            }
            return resource;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read stored image", exception);
        }
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(resolveKey(key));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete stored image", exception);
        }
    }

    private long copyWithLimit(InputStream input, Path destination) throws IOException {
        try (input; var output = Files.newOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > maxFileSize) {
                    throw new BadRequestException("Image exceeds the maximum allowed size");
                }
                output.write(buffer, 0, count);
            }
            return total;
        }
    }

    private ImageFormat detectFormat(Path path) throws IOException {
        byte[] header = new byte[24];
        int length;
        try (InputStream input = Files.newInputStream(path)) {
            length = input.readNBytes(header, 0, header.length);
        }
        if (length >= 8
                && header[0] == (byte) 0x89 && header[1] == 'P' && header[2] == 'N'
                && header[3] == 'G' && header[4] == 0x0d && header[5] == 0x0a
                && header[6] == 0x1a && header[7] == 0x0a) {
            int[] dimensions = readImageDimensions(path);
            return new ImageFormat("image/png", "png", dimensions[0], dimensions[1]);
        }
        if (length >= 3 && header[0] == (byte) 0xff && header[1] == (byte) 0xd8
                && header[2] == (byte) 0xff) {
            int[] dimensions = readImageDimensions(path);
            return new ImageFormat("image/jpeg", "jpg", dimensions[0], dimensions[1]);
        }
        if (length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F'
                && header[3] == 'F' && header[8] == 'W' && header[9] == 'E'
                && header[10] == 'B' && header[11] == 'P') {
            if (length < 16 || Files.size(path) < 20
                    || Integer.toUnsignedLong(readLittleEndianInt(header, 4)) != Files.size(path) - 8) {
                throw new BadRequestException("Image contents are invalid");
            }
            String chunk = new String(header, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (!chunk.equals("VP8 ") && !chunk.equals("VP8L") && !chunk.equals("VP8X")) {
                throw new BadRequestException("Image contents are invalid");
            }
            return new ImageFormat("image/webp", "webp", null, null);
        }
        throw new BadRequestException("Only valid JPEG, PNG, and WebP images are supported");
    }

    private Path resolveKey(String key) {
        if (key == null || !key.matches("[0-9a-fA-F-]{36}\\.(jpg|png|webp)")) {
            throw new BadRequestException("Invalid image storage key");
        }
        Path resolved = root.resolve(key).normalize();
        if (!resolved.getParent().equals(root)) {
            throw new BadRequestException("Invalid image storage key");
        }
        return resolved;
    }

    private int[] readImageDimensions(Path path) {
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) {
                throw new BadRequestException("Image contents are invalid");
            }
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BadRequestException("Image contents are invalid");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 12000 || height > 12000) {
                    throw new BadRequestException("Image dimensions are invalid or too large");
                }
                return new int[] {width, height};
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new BadRequestException("Image contents are invalid");
        } catch (RuntimeException exception) {
            if (exception instanceof BadRequestException badRequestException) {
                throw badRequestException;
            }
            throw new BadRequestException("Image contents are invalid");
        }
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff)
                | (bytes[offset + 1] & 0xff) << 8
                | (bytes[offset + 2] & 0xff) << 16
                | (bytes[offset + 3] & 0xff) << 24;
    }

    private record ImageFormat(String mimeType, String extension, Integer width, Integer height) {
    }
}

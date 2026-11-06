package com.designer.costume.service;

public record StoredImage(
        String storageKey,
        String mimeType,
        long fileSize,
        String extension,
        Integer imageWidth,
        Integer imageHeight) {
}

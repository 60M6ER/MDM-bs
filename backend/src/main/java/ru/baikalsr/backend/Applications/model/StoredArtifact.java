package ru.baikalsr.backend.Applications.model;

/**
 * Результат сохранения файла артефакта в storage.
 */
public record StoredArtifact(
        String relativePath,
        String originalFilename,
        String storedFilename,
        String contentType,
        long sizeBytes,
        String sha256
) {}

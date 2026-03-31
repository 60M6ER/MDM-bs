package ru.baikalsr.backend.Applications.dto;

import java.time.Instant;

/**
 * Детальное представление релиза приложения.
 */
public record ArtifactReleaseDetailsDto(
        Long id,
        Long appId,
        Integer versionCode,
        String versionName,
        String sha256,
        Long sizeBytes,
        String storagePath,
        String storageType,
        Instant uploadedAt,
        String uploadedBy,
        String notes
) {}

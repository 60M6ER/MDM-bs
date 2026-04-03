package ru.baikalsr.backend.Applications.dto;

import java.time.Instant;

/**
 * Публичное DTO для проверки актуальной версии приложения по packageName.
 */
public record ApplicationCurrentVersionDto(
        String packageName,
        Integer versionCode,
        String versionName,
        String sha256,
        Long sizeBytes,
        Instant uploadedAt
) {}

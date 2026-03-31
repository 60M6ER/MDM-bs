package ru.baikalsr.backend.Applications.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Запрос на создание нового релиза приложения.
 */
public record ArtifactReleaseCreateRequest(
        @NotNull
        Long appId,

        @Size(max = 128)
        String uploadedBy,

        String notes
) {}

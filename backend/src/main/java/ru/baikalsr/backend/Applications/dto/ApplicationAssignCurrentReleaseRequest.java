package ru.baikalsr.backend.Applications.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Запрос на назначение текущего релиза приложения.
 */
public record ApplicationAssignCurrentReleaseRequest(
        @NotNull
        Long releaseId,

        @Size(max = 128)
        String assignedBy
) {}

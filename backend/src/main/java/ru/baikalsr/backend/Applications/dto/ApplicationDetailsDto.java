package ru.baikalsr.backend.Applications.dto;

import java.time.Instant;

public record ApplicationDetailsDto(
        Long id,
        String key,
        String name,
        String packageName,
        boolean active,
        Instant createdAt,
        boolean canReset
) {}

package ru.baikalsr.backend.Applications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationCreateRequest(
        @NotBlank
        @Size(max = 64)
        String key,

        @NotBlank
        @Size(max = 255)
        String name
) {}
